/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ott;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class TVPAPIAnalyticsEvent implements PKEvent {

    public static final Class<TVPAPIAnalyticsReport> reportSent = TVPAPIAnalyticsReport.class;

    public enum Type {
        REPORT_SENT
    }

    public static class TVPAPIAnalyticsReport extends TVPAPIAnalyticsEvent {

        public final String reportedEventName;

        public TVPAPIAnalyticsReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }
    }
    
    @Override
    public Enum eventType() {
        return Type.REPORT_SENT;
    }
}
