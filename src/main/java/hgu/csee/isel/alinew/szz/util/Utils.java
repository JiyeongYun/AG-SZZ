package hgu.csee.isel.alinew.szz.util;

import java.util.List;

import org.eclipse.jdt.core.dom.Comment;

import hgu.csee.isel.alinew.szz.model.Line;

public class Utils {

	public static boolean isWhitespace(String str) {
		return str.replaceAll("\\s", "").equals("");
	}

	public static String mergeLineList(List<Line> list) {
		String mergedContent = "";

		for (Line line : list) {
			mergedContent += line.getContent();
		}

		return mergedContent.replaceAll("\\s", "");
	}
	
	public static String removeComments(String code) {

		JavaASTParser codeAST = new JavaASTParser(code);
		@SuppressWarnings("unchecked")
		List<Comment> lstComments = codeAST.cUnit.getCommentList();

		for(Comment comment:lstComments){
			code = replaceComments(code,comment.getStartPosition(),comment.getLength());
		}

		return code;
	}
	
	private static String replaceComments(String code, int startPosition, int length) {

		String pre = code.substring(0,startPosition);
		String post = code.substring(startPosition+length,code.length());

		String comments = code.substring(startPosition, startPosition+length);

		comments = comments.replaceAll("\\S"," ");

		code = pre + comments + post;

		return code;
	}
}
