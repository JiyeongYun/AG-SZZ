package AG.SZZ;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentTest {

	public static void main(String[] args) {
		int i = 0;   
		String comment1 = "  // 		str1  ";
		String comment2 = "  //* str2  ";
		String comment3 = "  */ 		str3  ";
		String comment4 = "  *********str4  ";
		String comment5 = " * str5  ";
		String comment6 = " /////////str6";
		String comment7 = " this is not comment!      ";
		String comment8 = "  //      str8";
		String comment9 = " /** 	str9";
		String comment10 = "  str10 */";
		String comment11 = "  str11 ***/";
		String comment12 = "   hello    ";
		String comment13 = " c = a * b";
		String comment14 = "c = a / b";
		String comment15 = "//					for(Line line : childLineList) {";
		
		ArrayList<String> arr = new ArrayList<>();
		
		arr.add(comment1); arr.add(comment2); arr.add(comment3); arr.add(comment4);
		arr.add(comment5); arr.add(comment6); arr.add(comment7); arr.add(comment8);
		arr.add(comment9); arr.add(comment10); arr.add(comment11); arr.add(comment12);
		arr.add(comment13); arr.add(comment14); arr.add(comment15);
		
//		str1 = str1.replaceAll( "[*]*/.*|[*]*|/.*|//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1" );

		String str1 = "				  	 	 		    	     		  	 			   					";
		String str2 = "  int i    	 =	0;";
		String str3 = "    import     java.util.regex.Pattern;";
		
		System.out.println(str1.replaceAll("\\s+", ""));
		System.out.println(str2.replaceAll("\\s+", ""));
		System.out.println(str3.replaceAll("\\s+", ""));
		
		/**
		 * whitespace 01
		 */
		
//		str1 = str1.replaceAll("\\s+", "");
//		
//		if(str1.equals("")) {
//			System.out.println("This is not BIC! This string is whitespace.\n");
//		} else {
//			System.out.println("This string is available as a BIC candidate.\n");
//		}
		
		
		/**
		 * whitespace 02
		 */
		
		boolean isBIC = false;
//
	    Pattern pattern = Pattern.compile("\\S");
	    Matcher matcher = pattern.matcher(str2);
//	        
//	    while (matcher.find()) {
//	    	isBIC = true;			//????????? ????????? ???????????? ???    
//	    	break;
//	    }
//	        
//	    if(isBIC) {
//	    	System.out.println("This string is available as a BIC candidate.\n");
//	    } else {
//	    	System.out.println("This is not BIC! This string is whitespace.\n");
//	    }
		
	
		/**
		 * import 
		 */
	    
//		if(str3.trim().split(" ")[0].equals("import")) {
//			System.out.println("This is not BIC! This is import statement.\n");
//		} else{
//			System.out.println("This string is available as a BIC candidate.\n");
//		}
		
		/**
		 * comment
		 */
		
		isBIC = true;
		System.out.println("=========comment========");

//	    pattern = Pattern.compile("(((\\s*\\/+\\**)|(\\s*\\*+)|(.*\\*+\\/*))+.*)");
	    pattern = Pattern.compile("(^((\\s*\\/+\\**)|(\\s*\\*+)|(.*\\*+\\/+)).*)");
		
	    
	    for(String str : arr) {
			isBIC = true;
	    	System.out.println(str);
	    	matcher = pattern.matcher(str);
		    
		    while (matcher.find()) {
		    	isBIC = false;		    
		    	break;
		    }
		        
		    if(isBIC) {
		    	System.out.println("This string is available as a BIC candidate.\n");
		    } else {
		    	System.out.println("This is not BIC! This string is comment.\n");
		    }
	    }
	    
		
	}

}
