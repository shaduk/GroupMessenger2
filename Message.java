package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by shadkhan on 23/03/17.
 */

public class Message {

    public String mContent;
    public String mType;
    public int mId;
    public int mProcess;
    public int kSuggestingProcess;
    public boolean mStatus;
    public int mPriority;
    public String failP;

    public Message(){
        this.mContent = "";
        this.mStatus = false;
        this.failP = "";
    }

    public static Comparator<Message> PriorityComparator = new Comparator<Message>() {

        public int compare(Message x, Message y)
        {
            if(x.mPriority == y.mPriority) {
                return x.kSuggestingProcess - y.kSuggestingProcess;
            }
            else
            {
                return x.mPriority-y.mPriority;
            }
        }
    };

    public String toString() {
        return mType + "-"+ mPriority + "-" + mStatus + "-" + mId + "-" + mContent + "-" + mProcess + "-" + kSuggestingProcess+"-"+failP+"-\n";
    }
}
