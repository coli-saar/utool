local repository = "https://github.com/coli-saar/utool/"

local repository_targets = {
  ["tools/client"] = repository .. "tree/master/tools/client",
  ["tools/lkb"] = repository .. "tree/master/tools/lkb",
  ["examples/erg-equivalences.xml"] = repository .. "blob/master/src/main/resources/examples/erg-equivalences.xml",
  ["de.saar.chorus.domgraph.codec"] = repository .. "tree/master/src/main/java/de/saar/chorus/domgraph/codec",
  ["de/saar/chorus/domgraph/codec/codecclasses.properties"] = repository .. "blob/master/src/main/resources/de/saar/chorus/domgraph/codec/codecclasses.properties"
}

local non_links = {
  ["build.xml"] = true,
  ["codecclasses.properties"] = true,
  ["de.saar.chorus.domgraph.equivalence"] = true,
  ["erg/gold/rondane"] = true,
  ["foo.bar.MyCodec"] = true
}

function Link(link)
  if repository_targets[link.target] then
    link.target = repository_targets[link.target]
    return link
  end

  if non_links[link.target] then
    return link.content
  end

  if link.target:match("^[^/@]+@[^/@]+$") then
    link.target = "mailto:" .. link.target
    return link
  end

  return link
end
