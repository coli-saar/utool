package de.saar.chorus.domgraph.codec;


class CodecManagerTest extends GroovyTestCase {
	 private CodecManager manager;
     
     @Configuration(beforeSuite = true)
     public void setUp() throws Exception {
         manager = new CodecManager();
         manager.registerAllDeclaredCodecs();
     }
     
     public void testGetCodecNames() {
         assert "domcon-oz".equals(manager.getInputCodecNameForFilename("foo.clls"));
         assert "domcon-oz".equals(manager.getOutputCodecNameForFilename("foo.clls"));
         assert "mrs-prolog".equals(manager.getInputCodecNameForFilename("foo.mrs.pl"));
         assert "domgraph-udraw".equals(manager.getOutputCodecNameForFilename("foo.dg.udg"));
         
         assert manager.getOutputCodecNameForFilename("does.not.exist") == null;
     }
     
     public void testGetParameterDefaultValues() {
     	assert "false".equals(manager.getOutputCodecParameterDefaultValue("domgraph-udraw", "pipe"));
     	assert manager.getOutputCodecParameterDefaultValue("domgraph-udraw", "does not exist") == null;
     	assert manager.getOutputCodecParameterDefaultValue("does not exist", "foo") == null;
     }

}
