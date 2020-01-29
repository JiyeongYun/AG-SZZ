package hgu.csee.isel.alinew.szz.model;

import java.util.List;

public class Line {
	private String path;
	private String rev;
	private LineType lineType;
	private List<Line> ancestors;
	
	public Line(String path, String rev, LineType lineType, List<Line> ancestors) {
		super();
		this.path = path;
		this.rev = rev;
		this.lineType = lineType;
		this.ancestors = ancestors;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	public LineType getLineType() {
		return lineType;
	}

	public void setLineType(LineType lineType) {
		this.lineType = lineType;
	}

	public List<Line> getAncestors() {
		return ancestors;
	}

	public void setAncestors(List<Line> ancestors) {
		this.ancestors = ancestors;
	}

}
