use super::{CodecError, CodecResult};
use crate::graph::GraphBuilder;
use quick_xml::Reader;
use quick_xml::events::{BytesStart, Event};

#[derive(Default)]
struct NodeRecord {
    id: String,
    kind: String,
    label: Option<String>,
}

#[derive(Default)]
struct EdgeRecord {
    source: String,
    target: String,
    kind: String,
}

fn attribute(element: &BytesStart<'_>, name: &[u8]) -> Result<Option<String>, CodecError> {
    for attribute in element.attributes() {
        let attribute = attribute.map_err(|error| CodecError::Syntax(error.to_string()))?;
        if attribute.key.as_ref() == name {
            return Ok(Some(
                attribute
                    .unescape_value()
                    .map_err(|error| CodecError::Syntax(error.to_string()))?
                    .into_owned(),
            ));
        }
    }
    Ok(None)
}

pub(super) fn append_reference(
    reference: &quick_xml::events::BytesRef<'_>,
    output: &mut String,
) -> Result<(), CodecError> {
    if let Some(character) = reference
        .resolve_char_ref()
        .map_err(|error| CodecError::Syntax(error.to_string()))?
    {
        output.push(character);
        return Ok(());
    }
    let name = reference
        .decode()
        .map_err(|error| CodecError::Syntax(error.to_string()))?;
    output.push_str(match name.as_ref() {
        "amp" => "&",
        "lt" => "<",
        "gt" => ">",
        "quot" => "\"",
        "apos" => "'",
        other => return Err(CodecError::Syntax(format!("unknown XML entity &{other};"))),
    });
    Ok(())
}

/// Parse Java Utool's dominance-graph GXL dialect.
///
/// # Errors
///
/// Returns a syntax error for malformed XML and a semantic error for invalid
/// node declarations.
#[allow(clippy::too_many_lines)]
pub fn parse_domgraph_gxl(input: &str) -> CodecResult {
    let mut reader = Reader::from_str(input);
    reader.config_mut().trim_text(true);
    let mut nodes = Vec::<NodeRecord>::new();
    let mut edges = Vec::<EdgeRecord>::new();
    let mut current_node = None::<NodeRecord>;
    let mut current_edge = None::<EdgeRecord>;
    let mut label_attr = false;
    let mut in_string = false;
    let mut label_text = String::new();

    loop {
        match reader.read_event() {
            Ok(Event::Start(element)) => match element.local_name().as_ref() {
                b"node" => {
                    current_node = Some(NodeRecord {
                        id: attribute(&element, b"id")?
                            .ok_or_else(|| CodecError::Syntax("GXL node has no id".to_owned()))?,
                        ..NodeRecord::default()
                    });
                }
                b"edge" => {
                    current_edge = Some(EdgeRecord {
                        source: attribute(&element, b"from")?.ok_or_else(|| {
                            CodecError::Syntax("GXL edge has no from attribute".to_owned())
                        })?,
                        target: attribute(&element, b"to")?.ok_or_else(|| {
                            CodecError::Syntax("GXL edge has no to attribute".to_owned())
                        })?,
                        ..EdgeRecord::default()
                    });
                }
                b"type" => {
                    let kind = attribute(&element, b"xlink:href")?
                        .or(attribute(&element, b"href")?)
                        .unwrap_or_default();
                    if let Some(node) = &mut current_node {
                        node.kind = kind;
                    } else if let Some(edge) = &mut current_edge {
                        edge.kind = kind;
                    }
                }
                b"attr" => label_attr = attribute(&element, b"name")?.as_deref() == Some("label"),
                b"string" if label_attr => {
                    in_string = true;
                    label_text.clear();
                }
                _ => {}
            },
            Ok(Event::Empty(element)) if element.local_name().as_ref() == b"type" => {
                let kind = attribute(&element, b"xlink:href")?
                    .or(attribute(&element, b"href")?)
                    .unwrap_or_default();
                if let Some(node) = &mut current_node {
                    node.kind = kind;
                } else if let Some(edge) = &mut current_edge {
                    edge.kind = kind;
                }
            }
            Ok(Event::Text(text)) if in_string => {
                label_text.push_str(
                    &text
                        .decode()
                        .map_err(|error| CodecError::Syntax(error.to_string()))?,
                );
            }
            Ok(Event::GeneralRef(reference)) if in_string => {
                append_reference(&reference, &mut label_text)?;
            }
            Ok(Event::End(element)) => {
                match element.local_name().as_ref() {
                    b"string" if in_string => {
                        if let Some(node) = &mut current_node {
                            node.label = Some(label_text.clone());
                        }
                        in_string = false;
                    }
                    b"attr" => label_attr = false,
                    b"node" => nodes.push(current_node.take().ok_or_else(|| {
                        CodecError::Syntax("GXL node end without start".to_owned())
                    })?),
                    b"edge" => edges.push(current_edge.take().ok_or_else(|| {
                        CodecError::Syntax("GXL edge end without start".to_owned())
                    })?),
                    _ => {}
                }
            }
            Ok(Event::Eof) => break,
            Ok(_) => {}
            Err(error) => return Err(CodecError::Syntax(format!("invalid GXL XML: {error}"))),
        }
    }

    let mut builder = GraphBuilder::default();
    for node in nodes {
        let id = builder.ensure_node(&node.id);
        if node.kind != "hole" {
            builder.set_label(
                id,
                node.label.ok_or_else(|| {
                    CodecError::Semantic(format!("labeled GXL node {:?} has no label", node.id))
                })?,
            )?;
        }
    }
    for edge in edges {
        let source = builder.ensure_node(edge.source);
        let target = builder.ensure_node(edge.target);
        if edge.kind == "solid" {
            builder.add_tree_edge(source, target);
        } else {
            builder.add_dominance_edge(source, target);
        }
    }
    Ok(builder.finish())
}
