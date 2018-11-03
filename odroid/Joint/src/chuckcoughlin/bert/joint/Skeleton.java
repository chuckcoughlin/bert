/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.joint;
public class Skeleton {

	public Skeleton() {
	}

	public String getInfo() {
		return "Control joints attached to robot body and extremeties";
	}

	public static void main(String[] args) {
		System.out.println(new Skeleton().getInfo());
	}

}
