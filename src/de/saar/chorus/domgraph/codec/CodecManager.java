/*
 * @(#)CodecManager.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class CodecManager {
    private List<OutputCodec> outputCodecs;
    private List<InputCodec> inputCodecs;
    
    public CodecManager() {
        outputCodecs = new ArrayList<OutputCodec>();
        inputCodecs = new ArrayList<InputCodec>();
    }
    
    public void registerCodec(Class codecClass) throws CodecRegistrationException 
    {
        try {
            if( OutputCodec.class.isAssignableFrom(codecClass) ) {
                Constructor constr;
                constr = codecClass.getConstructor(new Class[] { });
                OutputCodec codec = (OutputCodec) constr.newInstance(new Object[] { });
                outputCodecs.add(codec);
            } else if( InputCodec.class.isAssignableFrom(codecClass)) {
                Constructor constr;
                constr = codecClass.getConstructor(new Class[] { });
                InputCodec codec = (InputCodec) constr.newInstance(new Object[] { });
                inputCodecs.add(codec);
            }
        } catch (Exception e) {
            throw new CodecRegistrationException(e);
        } 
    }
    
    public InputCodec getInputCodecForName(String codecname) {
        for( InputCodec codec : inputCodecs ) {
            if( codecname.equals(codec.getName())) {
                return codec;
            }
        }
        
        return null;
    }
    
    public InputCodec getInputCodecForFilename(String filename) {
        for( InputCodec codec : inputCodecs ) {
            if( codec.getExtension() != null ) {
                if( filename.endsWith(codec.getExtension())) {
                    return codec;
                }
            }
        }
        
        return null;
    }
    
    
    public OutputCodec getOutputCodecForName(String codecname) {
        for( OutputCodec codec : outputCodecs ) {
            if( codecname.equals(codec.getName())) {
                return codec;
            }
        }
        
        return null;
    }
    
    public OutputCodec getOutputCodecForFilename(String filename) {
        for( OutputCodec codec : outputCodecs ) {
            if( filename.endsWith(codec.getExtension())) {
                return codec;
            }
        }
        
        return null;
    }

    public void displayAllCodecs(PrintStream out) {
        int max_strlen = 0;
        String formatString, formatStringNoExt;
        
        for( InputCodec codec : inputCodecs ) {
            max_strlen = Math.max(max_strlen, codec.getName().length());
        }

        for( OutputCodec codec : outputCodecs ) {
            max_strlen = Math.max(max_strlen, codec.getName().length());
        }
        
        formatString = "    %1$-" + max_strlen + "s             (%2$s)";
        formatStringNoExt = "    %1$s";
        

        
        out.println("Installed input codecs:");
        for( InputCodec codec : inputCodecs ) {
            if( codec.getExtension() == null ) {
                out.println(String.format(formatStringNoExt, codec.getName()));
            } else {
                out.println(String.format(formatString, codec.getName(), codec.getExtension()));
            }
        }
        
        out.println("\nInstalled output codecs:");
        for( OutputCodec codec : outputCodecs ) {
            if( codec.getExtension() == null ) {
                out.println(String.format(formatStringNoExt, codec.getName()));
            } else {
                out.println(String.format(formatString, codec.getName(), codec.getExtension()));
            }
        }
    }

    public List<InputCodec> getAllInputCodecs() {
        return inputCodecs;
    }

    public List<OutputCodec> getAllOutputCodecs() {
        return outputCodecs;
    }

}
