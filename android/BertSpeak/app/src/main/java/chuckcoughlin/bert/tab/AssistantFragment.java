/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.tab;


import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * This interface is required for all fragments in the application.
 */

public interface AssistantFragment {

    String PAGE_ARG = "page";
    String TITLE_ARG= "title";

    int getPageNumber();
    void setPageNumber(int page);
    String getTitle();
    void setTitle(String title);

}
