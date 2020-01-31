package hgu.csee.isel.alinew.szz.model;

import org.eclipse.jgit.diff.Edit.Type;

public class Hunk {
	private String diffType;
	private int beginA;
	private int endA;
	private int beginB;
	private int endB;

	public Hunk(String diffType, int beginA, int endA, int beginB, int endB) {
		super();
		this.diffType = diffType;
		this.beginA = beginA;
		this.endA = endA;
		this.beginB = beginB;
		this.endB = endB;
	}

	public String getDiffType() {
		return diffType;
	}
	
	public int getBeginA() {
		return beginA;
	}
	
	public int getEndA() {
		return endA;
	}
	
	public int getBeginB() {
		return beginB;
	}
	
	public int getEndB() {
		return endB;
	}
	
	public int getRangeA() {
		return Math.abs(this.beginA - this.endA);
	}
	
	public int getRangeB() {
		return Math.abs(this.beginB - this.endB);
	}

	
}
