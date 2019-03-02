/**
 *   (c) 2019  Charles Coughlin. All rights reserved.
 *   (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * This enumeration class represents the permissible states of ordered actions
 * that make up the voice service.
 */
public enum ActionState {
	IDLE,
    WAITING,
    ACTIVE,
    ERROR
	;

	/**
	 * @return  a comma-separated list of all state values in a single String.
	 */
	public static String names() {
		StringBuffer names = new StringBuffer();
		for (ActionState state : ActionState.values())
		{
			names.append(state.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
