/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.control;

/**
 * These are the canonical names for the links of the humanoid.
 * We try to be anatomically correct, but don't always succeed.
 */
public enum Limb
{
	BACK,
	CERVICAL,
	HEAD,
	LEFT_CLAVICLE,
	LEFT_FOOT,
	LEFT_FOREARM,
	LEFT_HIP_LINK,
	LEFT_HIP_SOCKET,
	LEFT_ILLIUM,
	LEFT_SHIN,
	LEFT_SHOULDER_LINK,
	LEFT_THIGH,
	LEFT_UPPER_ARM,
	LOWER_SPINE,
	LUMBAR,
	PELVIS,
	RIGHT_CLAVICLE,
	RIGHT_FOOT,
	RIGHT_FOREARM,
	RIGHT_HIP_LINK,
	RIGHT_HIP_SOCKET,
	RIGHT_ILLIUM,
	RIGHT_SHIN,
	RIGHT_SHOULDER_LINK,
	RIGHT_THIGH,
	RIGHT_UPPER_ARM,
	SPINE,
	THORACIC,
	UNKNOWN
	;

	/**
	 * Convert the Limb enumeration to text that can be pronounced.
	 * @param limb the enumeration
	 * @return user-recognizable text
	 */
	public static String toText(Limb limb) {
		String text = "";
		switch( limb ) {
			case BACK: text = "back"; break;
			case CERVICAL: text = "cervical vertibrae"; break;
			case HEAD: text = "head"; break;
			case LEFT_CLAVICLE: text = "left collar bone"; break;
			case LEFT_FOOT: text = "left foot"; break;
			case LEFT_FOREARM: text = "left forearm"; break;
			case LEFT_HIP_LINK: text = "left hip link"; break;
			case LEFT_HIP_SOCKET: text = "left hip socket"; break;
			case LEFT_ILLIUM: text = "left illium"; break;
			case LEFT_SHIN: text = "left shin"; break;
			case LEFT_SHOULDER_LINK: text = "left shoulder link"; break;
			case LEFT_THIGH: text = "left thigh"; break;
			case LEFT_UPPER_ARM: text = "left upper arm"; break;
			case LOWER_SPINE: text = "lower spine"; break;
			case LUMBAR: text = "lumbar"; break;
			case PELVIS: text = "pelvis"; break;
			case RIGHT_CLAVICLE: text = "right collar bone"; break;
			case RIGHT_FOOT: text = "right foot"; break;
			case RIGHT_FOREARM: text = "right forearm"; break;
			case RIGHT_HIP_LINK: text = "right hip link"; break;
			case RIGHT_HIP_SOCKET: text = "right hip socket"; break;
			case RIGHT_ILLIUM: text = "right illium"; break;
			case RIGHT_SHIN: text = "right shin"; break;
			case RIGHT_SHOULDER_LINK: text = "right shoulder link"; break;
			case RIGHT_THIGH: text = "right thigh"; break;
			case RIGHT_UPPER_ARM: text = "right upper arm"; break;
			case SPINE: text = "spine"; break;
			case THORACIC: text = "thoracic vertibrae"; break;
			case UNKNOWN: text = "unknown"; break;
		}
		return text;
	}
	/**
	 * @return  a comma-separated list of all block states in a single String.
	 */
	public static String names()
	{
		StringBuffer names = new StringBuffer();
		for (Limb type : Limb.values())
		{
			names.append(type.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
