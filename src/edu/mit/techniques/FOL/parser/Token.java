package edu.mit.techniques.FOL.parser;

class Token {

	private String name;
	private boolean isSymbol;
		
	public Token (String _name, boolean _isSymbol){
		name = _name;
		isSymbol = _isSymbol;
	}
	
	// Too lazy to make all these subclasses.  So we'll just make constants.
	// Any token that isn't one of these is a word.
	
	public static final Token comma = new Token("Comma", false);
	public static final Token singleArrow = new Token("singleArrow", false);
	public static final Token doubleArrow = new Token("doubleArrow", false);
	public static final Token vee = new Token("vee", false);
	public static final Token wedge = new Token("wedge", false);
	public static final Token neg = new Token("neg", false);
	public static final Token all = new Token("all", false);
	public static final Token exists = new Token("exists", false);
	public static final Token leftParen = new Token("leftParen", false);
	public static final Token rightParen = new Token("rightParen", false);
	public static final Token t = new Token("t", false);
	public static final Token f = new Token("f", false);
	
	public static final Token eol = new Token("eol", false);
	
	public String name(){
		return name;
	}
	
	public boolean isSymbol(){
		return isSymbol;
	}
	
	public String toString(){
		return name.toString();
	}
	
	
}
