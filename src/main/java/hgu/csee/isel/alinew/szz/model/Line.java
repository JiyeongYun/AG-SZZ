package hgu.csee.isel.alinew.szz.model;

import java.util.List;

public class Line {
	private String path;
	private String rev;
	private String content;
	private int idx;
	private LineType lineType;
	private List<Line> ancestors;

	public Line(String path, String rev, String content, int idx, List<Line> ancestors) {
		this(path, rev, content, idx, LineType.CONTEXT, ancestors);
	}

	public Line(String path, String rev, String content, int idx, LineType lineType, List<Line> ancestors) {
		super();
		this.path = path;
		this.rev = rev;
		this.content = content;
		this.idx = idx;
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
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
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
