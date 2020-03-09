package hgu.csee.isel.alinew.szz.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

		for (Comment comment : lstComments) {
			code = replaceComments(code, comment.getStartPosition(), comment.getLength());
		}

		return code;
	}

	private static String replaceComments(String code, int startPosition, int length) {

		String pre = code.substring(0, startPosition);
		String post = code.substring(startPosition + length, code.length());

		String comments = code.substring(startPosition, startPosition + length);

		comments = comments.replaceAll("\\S", " ");

		code = pre + comments + post;

		return code;
	}

	public static String getStringDateTimeFromCommitTime(int commitTime) {
		SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date commitDate = new Date(commitTime * 1000L);

		TimeZone GMT = TimeZone.getTimeZone("GMT");
		ft.setTimeZone(GMT);

		return ft.format(commitDate);
	}

	public static void storeOutputFile(String GIT_URL, List<Line> BICLines) throws IOException {
		// Set file name
		String[] arr = GIT_URL.split("/");
		String projName = arr[arr.length - 1];
		String fName = System.getProperty("user.dir") + File.separator + "results" + File.separator + projName + ".csv";
		
		File savedFile = new File(fName);
		savedFile.getParentFile().mkdirs();
		
		FileWriter writer = new FileWriter(savedFile);

		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Path", "BIC", "Content", "Line Idx", "Committer", "Author", "Commit Date(GMT)"));

		for (Line BICLine : BICLines) {
			csvPrinter.printRecord(BICLine.getPath(), BICLine.getRev(), BICLine.getContent(), BICLine.getIdx(), BICLine.getCommiter(), BICLine.getAuthor(), BICLine.getCommitDate());
		}
		
		csvPrinter.close();
	}
}
