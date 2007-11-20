package de.saar.basic;


class UnionFindTest extends GroovyTestCase {
	public void test1() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    uf.union("a", "b");
	    uf.union("a", "c");
	    
	    assert uf.find("a") == uf.find("b");
	    assert uf.find("a") == uf.find("c");
	}
    
	public void test2() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    uf.union("a", "c");
	    uf.union("a", "b");
	    
	    assert uf.find("a") == uf.find("b");
	    assert uf.find("a") == uf.find("c");
	}

	public void test3() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    uf.union("c", "a");
	    uf.union("b", "a");
	    
	    assert uf.find("a") == uf.find("b");
	    assert uf.find("a") == uf.find("c");
	}

	public void test4() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    uf.union("c", "a");
	    uf.union("b", "c");
	    
	    assert uf.find("a") == uf.find("b");
	    assert uf.find("a") == uf.find("c");
	}
	
	public void testNotNull() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    uf.union("c", "a");
	    uf.union("b", "c");
	    
	    assert uf.find("a") != null;
	    assert uf.find("b") != null;
	    assert uf.find("c") != null;
	    assert uf.find("d") != null;
	}
	
	public void testInitialState() {
	    UnionFind uf = new UnionFind(["a", "b", "c", "d"]);
	    
	    assert uf.find("a") == "a";
	    assert uf.find("b") == "b";
	    assert uf.find("c") == "c";
	    assert uf.find("d") == "d";
	}

}