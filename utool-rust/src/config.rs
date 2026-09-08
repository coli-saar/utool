//! Shared user configuration stored in `~/.utool`.

use std::{
    collections::{HashMap, HashSet},
    fs,
    io::{self, Write},
    path::{Path, PathBuf},
};

pub const SERVER_PORT_PROPERTY: &str = "utool.server.port";
pub const SERVER_NON_LOCAL_PROPERTY: &str = "utool.server.accept-non-local";
pub const SERVER_AUTOSTART_PROPERTY: &str = "utool.server.start-on-launch";
pub const DEFAULT_INPUT_CODEC_PROPERTY: &str = "utool.codec.default.input";
pub const DEFAULT_OUTPUT_CODEC_PROPERTY: &str = "utool.codec.default.output";

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct CodecPreferences {
    pub default_input: String,
    pub default_output: String,
}

impl Default for CodecPreferences {
    fn default() -> Self {
        Self {
            default_input: "domcon-oz".to_owned(),
            default_output: "domcon-oz".to_owned(),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ServerPreferences {
    pub port: u16,
    pub accept_non_local: bool,
    pub start_on_launch: bool,
}

impl Default for ServerPreferences {
    fn default() -> Self {
        Self {
            port: 2802,
            accept_non_local: false,
            start_on_launch: false,
        }
    }
}

/// All properties loaded from the user's cross-platform `~/.utool` file.
#[derive(Clone, Debug)]
pub struct UserConfig {
    path: PathBuf,
    properties: HashMap<String, String>,
    updates: HashMap<String, String>,
}

impl UserConfig {
    /// Load `.utool` from the current user's home directory.
    ///
    /// # Errors
    ///
    /// Returns an error if the home directory cannot be resolved or the file
    /// cannot be read.
    pub fn load() -> io::Result<Self> {
        let home = dirs::home_dir().ok_or_else(|| {
            io::Error::new(io::ErrorKind::NotFound, "home directory is unavailable")
        })?;
        Self::load_from(home.join(".utool"))
    }

    fn load_from(path: PathBuf) -> io::Result<Self> {
        let contents = match fs::read_to_string(&path) {
            Ok(contents) => contents,
            Err(error) if error.kind() == io::ErrorKind::NotFound => String::new(),
            Err(error) => return Err(error),
        };
        let properties = contents
            .lines()
            .filter_map(parse_property)
            .map(|(key, value)| (key.to_owned(), value.to_owned()))
            .collect();
        let mut config = Self {
            path,
            properties,
            updates: HashMap::new(),
        };
        let codec_defaults = CodecPreferences::default();
        config.set_default_if_missing(DEFAULT_INPUT_CODEC_PROPERTY, codec_defaults.default_input);
        config.set_default_if_missing(DEFAULT_OUTPUT_CODEC_PROPERTY, codec_defaults.default_output);
        Ok(config)
    }

    /// Return any property, including properties only understood by Java Utool.
    pub fn get(&self, key: &str) -> Option<&str> {
        self.updates
            .get(key)
            .or_else(|| self.properties.get(key))
            .map(String::as_str)
    }

    #[must_use]
    pub fn server_preferences(&self) -> ServerPreferences {
        let defaults = ServerPreferences::default();
        ServerPreferences {
            port: self
                .get(SERVER_PORT_PROPERTY)
                .and_then(|value| value.parse().ok())
                .unwrap_or(defaults.port),
            accept_non_local: self
                .get(SERVER_NON_LOCAL_PROPERTY)
                .map_or(defaults.accept_non_local, |value| {
                    value.eq_ignore_ascii_case("true")
                }),
            start_on_launch: self
                .get(SERVER_AUTOSTART_PROPERTY)
                .map_or(defaults.start_on_launch, |value| {
                    value.eq_ignore_ascii_case("true")
                }),
        }
    }

    #[must_use]
    pub fn codec_preferences(&self) -> CodecPreferences {
        let defaults = CodecPreferences::default();
        CodecPreferences {
            default_input: self
                .get(DEFAULT_INPUT_CODEC_PROPERTY)
                .unwrap_or(&defaults.default_input)
                .to_owned(),
            default_output: self
                .get(DEFAULT_OUTPUT_CODEC_PROPERTY)
                .unwrap_or(&defaults.default_output)
                .to_owned(),
        }
    }

    pub fn set_codec_preferences(&mut self, preferences: &CodecPreferences) {
        self.set(
            DEFAULT_INPUT_CODEC_PROPERTY,
            preferences.default_input.clone(),
        );
        self.set(
            DEFAULT_OUTPUT_CODEC_PROPERTY,
            preferences.default_output.clone(),
        );
    }

    pub fn set_server_preferences(&mut self, preferences: &ServerPreferences) {
        self.set(SERVER_PORT_PROPERTY, preferences.port.to_string());
        self.set(
            SERVER_NON_LOCAL_PROPERTY,
            preferences.accept_non_local.to_string(),
        );
        self.set(
            SERVER_AUTOSTART_PROPERTY,
            preferences.start_on_launch.to_string(),
        );
    }

    /// Save changed properties while preserving comments and unrelated settings.
    ///
    /// # Errors
    ///
    /// Returns an error if the configuration directory or file cannot be read
    /// or written.
    pub fn save(&self) -> io::Result<()> {
        let contents = match fs::read_to_string(&self.path) {
            Ok(contents) => contents,
            Err(error) if error.kind() == io::ErrorKind::NotFound => String::new(),
            Err(error) => return Err(error),
        };
        if let Some(parent) = self.path.parent() {
            fs::create_dir_all(parent)?;
        }
        atomic_write(
            &self.path,
            update_properties(&contents, &self.updates).as_bytes(),
        )
    }

    fn set(&mut self, key: &str, value: String) {
        self.updates.insert(key.to_owned(), value);
    }

    fn set_default_if_missing(&mut self, key: &str, value: String) {
        if !self.properties.contains_key(key) {
            self.set(key, value);
        }
    }
}

fn atomic_write(path: &Path, contents: &[u8]) -> io::Result<()> {
    let parent = path
        .parent()
        .filter(|parent| !parent.as_os_str().is_empty())
        .unwrap_or_else(|| Path::new("."));
    let mut temporary = tempfile::NamedTempFile::new_in(parent)?;
    temporary.write_all(contents)?;
    if let Ok(metadata) = fs::metadata(path) {
        temporary
            .as_file()
            .set_permissions(metadata.permissions())?;
    }
    temporary.as_file().sync_all()?;
    temporary.persist(path).map_err(|error| error.error)?;
    #[cfg(unix)]
    fs::File::open(parent)?.sync_all()?;
    Ok(())
}

fn parse_property(line: &str) -> Option<(&str, &str)> {
    let line = line.trim();
    if line.is_empty() || line.starts_with('#') || line.starts_with('!') {
        return None;
    }
    let separator = line.find(['=', ':'])?;
    Some((line[..separator].trim(), line[separator + 1..].trim()))
}

fn update_properties(contents: &str, updates: &HashMap<String, String>) -> String {
    let mut seen = HashSet::new();
    let mut lines = contents
        .lines()
        .map(|line| {
            let Some((key, _)) = parse_property(line) else {
                return line.to_owned();
            };
            updates.get(key).map_or_else(
                || line.to_owned(),
                |value| {
                    seen.insert(key.to_owned());
                    format!("{key}={value}")
                },
            )
        })
        .collect::<Vec<_>>();
    let mut additions = updates
        .iter()
        .filter(|(key, _)| !seen.contains(*key))
        .collect::<Vec<_>>();
    additions.sort_unstable_by(|left, right| left.0.cmp(right.0));
    lines.extend(
        additions
            .into_iter()
            .map(|(key, value)| format!("{key}={value}")),
    );
    let mut result = lines.join("\n");
    result.push('\n');
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn updates_server_settings_without_disturbing_java_properties() {
        let existing = "# Utool\nutool.codec.default.input=domcon-oz\nutool.server.port=2802\n";
        let mut updates = HashMap::new();
        updates.insert(SERVER_PORT_PROPERTY.to_owned(), "2803".to_owned());
        updates.insert(SERVER_AUTOSTART_PROPERTY.to_owned(), "true".to_owned());
        let saved = update_properties(existing, &updates);

        assert!(saved.starts_with("# Utool\nutool.codec.default.input=domcon-oz\n"));
        assert!(saved.contains("utool.server.port=2803\n"));
        assert!(saved.contains("utool.server.start-on-launch=true\n"));
    }

    #[test]
    fn exposes_unknown_properties_through_one_configuration_object() {
        let path = std::env::temp_dir().join(format!("utool-config-test-{}", std::process::id()));
        fs::write(&path, "utool.future.setting=value\n").unwrap();
        let config = UserConfig::load_from(path.clone()).unwrap();
        assert_eq!(config.get("utool.future.setting"), Some("value"));
        fs::remove_file(path).unwrap();
    }

    #[test]
    fn supplies_and_saves_default_codec_specifications() {
        let path =
            std::env::temp_dir().join(format!("utool-codec-config-test-{}", std::process::id()));
        let config = UserConfig::load_from(path.clone()).unwrap();
        assert_eq!(config.codec_preferences(), CodecPreferences::default());
        config.save().unwrap();

        let saved = fs::read_to_string(&path).unwrap();
        assert!(saved.contains("utool.codec.default.input=domcon-oz\n"));
        assert!(saved.contains("utool.codec.default.output=domcon-oz\n"));
        fs::remove_file(path).unwrap();
    }

    #[cfg(unix)]
    #[test]
    fn saving_configuration_atomically_replaces_the_file() {
        use std::os::unix::fs::MetadataExt;

        let directory = tempfile::tempdir().unwrap();
        let path = directory.path().join(".utool");
        fs::write(&path, "utool.server.port=2802\n").unwrap();
        let original_inode = fs::metadata(&path).unwrap().ino();
        let mut config = UserConfig::load_from(path.clone()).unwrap();
        config.set_server_preferences(&ServerPreferences {
            port: 2803,
            ..ServerPreferences::default()
        });

        config.save().unwrap();

        assert_ne!(fs::metadata(&path).unwrap().ino(), original_inode);
        assert!(
            fs::read_to_string(path)
                .unwrap()
                .contains("utool.server.port=2803")
        );
    }
}
