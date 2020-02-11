package AG.SZZ;

import java.util.HashMap;

public class Main {

	public static void main(String[] args) {
		
		Person p1 = new Person("a", 1);
		Person p2 = new Person("b", 2);
		
		HashMap<Person, String> map = new HashMap();
		map.put(p1, "aaa");
		map.put(p2, "bbb");
		map.put(p1, "ccc");
		
		System.out.println(map.size());
		

	}

}
