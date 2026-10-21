package com.sigpher.xpdf.common.event;

/**
 * @author sigpher
 */
public class RecentPDFEvent {

    private boolean fromPreviewActivity;

    public RecentPDFEvent() {
    }

    public RecentPDFEvent(boolean fromPreviewActivity) {
        this.fromPreviewActivity = fromPreviewActivity;
    }

    public boolean isFromPreviewActivity() {
        return fromPreviewActivity;
    }

    public void setFromPreviewActivity(boolean fromPreviewActivity) {
        this.fromPreviewActivity = fromPreviewActivity;
    }
}
