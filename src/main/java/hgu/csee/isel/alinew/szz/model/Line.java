package hgu.csee.isel.alinew.szz.model;

import java.util.List;

public class Line {
	private String path;
	private String rev;
	private String content;
	private int idx;
	private LineType lineType;
	private List<Line> ancestors;
	private boolean isFormatChange;

	public Line(String path, String rev, String content, int idx, List<Line> ancestors) {
		this(path, rev, content, idx, LineType.CONTEXT, ancestors, false);
	}

	public Line(String path, String rev, String content, int idx, LineType lineType, List<Line> ancestors, boolean isFormatChange) {
		super();
		this.path = path;
		this.rev = rev;
		this.content = content;
		this.idx = idx;
		this.lineType = lineType;
		this.ancestors = ancestors;
		this.isFormatChange = isFormatChange;
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

	public boolean isFormatChange() {
		return isFormatChange;
	}

	public void setFormatChange(boolean isFormatChange) {
		this.isFormatChange = isFormatChange;
	}
}
