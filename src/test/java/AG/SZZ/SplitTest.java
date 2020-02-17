package AG.SZZ;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SplitTest {

	public static void main(String[] args) {
		String str = "/**\n" + 
				" * Licensed to the Apache Software Foundation (ASF) under one\n" + 
				" * or more contributor license agreements.  See the NOTICE file\n" + 
				" * distributed with this work for additional information\n" + 
				" * regarding copyright ownership.  The ASF licenses this file\n" + 
				" * to you under the Apache License, Version 2.0 (the\n" + 
				" * \"License\"); you may not use this file except in compliance\n" + 
				" * with the License.  You may obtain a copy of the License at\n" + 
				" *\n" + 
				" *     http://www.apache.org/licenses/LICENSE-2.0\n" + 
				" *\n" + 
				" * Unless required by applicable law or agreed to in writing,\n" + 
				" * software distributed under the License is distributed on an\n" + 
				" * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" + 
				" * KIND, either express or implied.  See the License for the\n" + 
				" * specific language governing permissions and limitations\n" + 
				" * under the License.\n" + 
				" */\n" + 
				"package org.apache.iotdb.tsfile;\n" + 
				"import java.io.IOException;\n" + 
				"import java.util.ArrayList;\n" + 
				"import org.apache.iotdb.tsfile.read.ReadOnlyTsFile;\n" + 
				"import org.apache.iotdb.tsfile.read.TsFileSequenceReader;\n" + 
				"import org.apache.iotdb.tsfile.read.common.Path;\n" + 
				"import org.apache.iotdb.tsfile.read.expression.IExpression;\n" + 
				"import org.apache.iotdb.tsfile.read.expression.QueryExpression;\n" + 
				"import org.apache.iotdb.tsfile.read.expression.impl.BinaryExpression;\n" + 
				"import org.apache.iotdb.tsfile.read.expression.impl.GlobalTimeExpression;\n" + 
				"import org.apache.iotdb.tsfile.read.expression.impl.SingleSeriesExpression;\n" + 
				"import org.apache.iotdb.tsfile.read.filter.TimeFilter;\n" + 
				"import org.apache.iotdb.tsfile.read.filter.ValueFilter;\n" + 
				"import org.apache.iotdb.tsfile.read.query.dataset.QueryDataSet;\n" + 
				"\n" + 
				"/**\n" + 
				" * The class is to show how to read TsFile file named \"test.tsfile\".\n" + 
				" * The TsFile file \"test.tsfile\" is generated from class TsFileWrite.\n" + 
				" * Run TsFileWrite to generate the test.tsfile first\n" + 
				" */\n" + 
				"public class TsFileRead {\n" + 
				"  private static void queryAndPrint(ArrayList<Path> paths, ReadOnlyTsFile readTsFile, IExpression statement)\n" + 
				"          throws IOException {\n" + 
				"    QueryExpression queryExpression = QueryExpression.create(paths, statement);\n" + 
				"    QueryDataSet queryDataSet = readTsFile.query(queryExpression);\n" + 
				"    while (queryDataSet.hasNext()) {\n" + 
				"      System.out.println(queryDataSet.next());\n" + 
				"    }\n" + 
				"    System.out.println(\"------------\");\n" + 
				"  }\n" + 
				"\n" + 
				"  public static void main(String[] args) throws IOException {\n" + 
				"\n" + 
				"    // file path\n" + 
				"    String path = \"test.tsfile\";\n" + 
				"\n" + 
				"    // create reader and get the readTsFile interface\n" + 
				"    TsFileSequenceReader reader = new TsFileSequenceReader(path);\n" + 
				"    ReadOnlyTsFile readTsFile = new ReadOnlyTsFile(reader);\n" + 
				"    // use these paths(all sensors) for all the queries\n" + 
				"    ArrayList<Path> paths = new ArrayList<>();\n" + 
				"    paths.add(new Path(\"device_1.sensor_1\"));\n" + 
				"    paths.add(new Path(\"device_1.sensor_2\"));\n" + 
				"    paths.add(new Path(\"device_1.sensor_3\"));\n" + 
				"\n" + 
				"    // no filter, should select 1 2 3 4 6 7 8\n" + 
				"    queryAndPrint(paths, readTsFile, null);\n" + 
				"\n" + 
				"    // time filter : 4 <= time <= 10, should select 4 6 7 8\n" + 
				"    IExpression timeFilter = BinaryExpression.and(new GlobalTimeExpression(TimeFilter.gtEq(4L)),\n" + 
				"            new GlobalTimeExpression(TimeFilter.ltEq(10L)));\n" + 
				"    queryAndPrint(paths, readTsFile, timeFilter);\n" + 
				"\n" + 
				"    // value filter : device_1.sensor_2 <= 20, should select 1 2 4 6 7\n" + 
				"    IExpression valueFilter = new SingleSeriesExpression(new Path(\"device_1.sensor_2\"),\n" + 
				"            ValueFilter.ltEq(20));\n" + 
				"    queryAndPrint(paths, readTsFile, valueFilter);\n" + 
				"\n" + 
				"    // time filter : 4 <= time <= 10, value filter : device_1.sensor_3 >= 20, should select 4 7 8\n" + 
				"    timeFilter = BinaryExpression.and(new GlobalTimeExpression(TimeFilter.gtEq(4L)),\n" + 
				"            new GlobalTimeExpression(TimeFilter.ltEq(10L)));\n" + 
				"    valueFilter = new SingleSeriesExpression(new Path(\"device_1.sensor_3\"), ValueFilter.gtEq(20));\n" + 
				"    IExpression finalFilter = BinaryExpression.and(timeFilter, valueFilter);\n" + 
				"    queryAndPrint(paths, readTsFile, finalFilter);\n" + 
				"\n" + 
				"    //close the reader when you left\n" + 
				"    reader.close();\n" + 
				"  }\n" + 
				"}";
		
		String str2 = "public class HelloWorldLineFeed{\n" + 
				"    System.out.println(\"Hello\");\n" + 
				"\n" + 
				"}\n" + 
				"\n" + 
				"";
		
		String str3 = str2.trim();
		
		
		Pattern pattern = Pattern.compile("\r\n|\r|\n");
	    Matcher matcher = pattern.matcher(str3);
			
		String[] arr = str3.split("\r\n|\r|\n");
		
		int lineCnt = 1;
		
		while (matcher.find()) {
			lineCnt ++;
		}
		
		System.out.println("Line Count : " + lineCnt);
		System.out.println("Length : " + arr.length);
		for(int i = 0; i < arr.length; i++) {
			System.out.println(arr[i]);
		}
	}
}
