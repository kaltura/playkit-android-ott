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

package com.kaltura.playkit.providers.base;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.playkit.PKPlaylist;

public interface OnPlaylistLoadCompletion extends OnCompletion<ResultElement<PKPlaylist>> {

}
