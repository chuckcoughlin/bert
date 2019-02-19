package chuckcoughlin.bertspeak.tab;


import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * This interface is required for all fragments in the application.
 */

public interface AssistantFragment {

    public final static String PAGE_ARG = "page";
    public final static String TITLE_ARG= "title";

    public int getPageNumber();
    public void setPageNumber(int page);
    public String getTitle();
    public void setTitle(String title);

}
