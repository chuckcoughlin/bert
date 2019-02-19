/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bert.common;


/**
 * Create a model class for various lists - a name/value pair.
 * Add a hint to help the user.
 */

public class NameValue {
    private String name = "";
    private String value = "";
    private String hint = "";

    public NameValue() {}

    public NameValue(String nam, String val,String desc) {
        this.name = nam;
        this.value = val;
        this.hint  = desc;
    }

    public String getHint() {
        return this.hint;
    }
    public String getName() {
        return this.name;
    }
    public String getValue() {
        return this.value;
    }

    public void setHint(String desc) {
        this.hint = desc;
    }
    public void setName(String nam) {
        this.name = nam;
    }
    public void setValue(String val) {
        this.value = val;
    }

    @Override
    public int hashCode () {
        return getName().hashCode()+getValue().hashCode();
    }
}
