package hgu.csee.isel.alinew.szz.model;

public enum LineType {
	INSERT,
	DELETE,
	REPLACE,
	CONTEXT;
	
	public String toString() {
		switch(this) {
			case INSERT: return "insert";
			case DELETE: return "delete";
			case REPLACE: return "replace";
			case CONTEXT: return "context";
			default: throw new IllegalArgumentException();
		}
	}

}
