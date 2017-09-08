// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import android.util.Pair;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HttpEvent extends DefaultEvent {

    private static final String TAG = HttpEvent.class.getSimpleName();

    HttpEvent(final String eventName) {
        getEventList().add(Pair.create(EventStrings.EVENT_NAME, eventName));
    }

    void setUserAgent(final String userAgent) {
        setProperty(EventStrings.HTTP_USER_AGENT, userAgent);
    }

    void setMethod(final String method) {
        setProperty(EventStrings.HTTP_METHOD, method);
    }

    void setQueryParameters(final String queryParameters) {
        setProperty(EventStrings.HTTP_QUERY_PARAMETERS, queryParameters);
    }

    void setResponseCode(final int responseCode) {
        setProperty(EventStrings.HTTP_RESPONSE_CODE, String.valueOf(responseCode));
    }

    void setApiVersion(final String apiVersion) {
        setProperty(EventStrings.HTTP_API_VERSION, apiVersion);
    }

    void setHttpPath(final URL httpPath) {
        final String authority = httpPath.getAuthority();
        final Discovery discovery = new Discovery();
        if (!discovery.getValidHosts().contains(authority)) {
            return;
        }

        final String[] splitArray = httpPath.getPath().split("/");

        final StringBuilder logPath = new StringBuilder();
        logPath.append(httpPath.getProtocol());
        logPath.append("://");
        logPath.append(authority);
        logPath.append("/");

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i]);
            logPath.append("/");
        }
        setProperty(EventStrings.HTTP_PATH, logPath.toString());
    }

    void setOauthErrorCode(final String errorCode) {
        setProperty(EventStrings.OAUTH_ERROR_CODE, errorCode);
    }

    void setRequestIdHeader(final String requestIdHeader) {
        setProperty(EventStrings.REQUEST_ID_HEADER, requestIdHeader);
    }

    /**
     * Parses and sets the relevant HttpEvent fields given x-ms-clitelem metadata.
     *
     * @param xMsCliTelem the value of the x-ms-clitelem header
     */
    void setXMsCliTelemData(final String xMsCliTelem) {
        // if the header isn't present, do nothing
        if (StringExtensions.isNullOrBlank(xMsCliTelem)) {
            return;
        }

        // split the header based on the delimiter
        String[] headerSegments = xMsCliTelem.split(",");

        // make sure the header isn't empty
        if (0 == headerSegments.length) {
            Logger.w(TAG, "SPE Ring header missing version field.", null, ADALError.X_MS_CLITELEM_VERSION_UNRECOGNIZED);
            return;
        }

        // get the version of this header
        final String headerVersion = headerSegments[0];

        // declare values tracked by this header
        String errorCode = null;
        String subErrorCode = null;
        String tokenAge = null;
        String speRing = null;

        if (headerVersion.equals("1")) {
            // The expected delimiter count of the v1 header
            final int delimCount = 4;

            // Verify the expected format "<version>, <error_code>, <sub_error_code>, <token_age>, <ring>"
            Pattern headerFmt = Pattern.compile("^[1-9]+\\.?[0-9|\\.]*,[0-9|\\.]*,[0-9|\\.]*,[^,]*[0-9\\.]*,[^,]*$");
            Matcher matcher = headerFmt.matcher(xMsCliTelem);
            if (!matcher.matches()) {
                Logger.w(TAG, "", "", ADALError.X_MS_CLITELEM_MALFORMED);
                return;
            }

            headerSegments = xMsCliTelem.split(",", delimCount + 1);

            final int indexErrorCode = 1;
            final int indexSubErrorCode = 2;
            final int indexTokenAge = 3;
            final int indexSpeInfo = 4;

            // get the error_code
            errorCode = headerSegments[indexErrorCode];

            // get the sub_error_code
            subErrorCode = headerSegments[indexSubErrorCode];

            // get the token_age
            tokenAge = headerSegments[indexTokenAge];

            // get the spe_ring
            speRing = headerSegments[indexSpeInfo];
        } else { // unrecognized version
            Logger.w(TAG, "Unexpected header version: " + headerVersion, null, ADALError.X_MS_CLITELEM_VERSION_UNRECOGNIZED);
        }
        // Set the extracted values on the HttpEvent
        if (!StringExtensions.isNullOrBlank(errorCode) && !errorCode.equals("0")) {
            setServerErrorCode(errorCode);
        }

        if (!StringExtensions.isNullOrBlank(subErrorCode) && !subErrorCode.equals("0")) {
            setServerSubErrorCode(subErrorCode);
        }

        if (!StringExtensions.isNullOrBlank(tokenAge)) {
            setRefreshTokenAge(tokenAge);
        }

        if (!StringExtensions.isNullOrBlank(speRing)) {
            setSpeRing(speRing);
        }
    }

    void setServerErrorCode(final String errorCode) {
        setProperty(EventStrings.SERVER_ERROR_CODE, errorCode.trim());
    }

    void setServerSubErrorCode(final String subErrorCode) {
        setProperty(EventStrings.SERVER_SUBERROR_CODE, subErrorCode.trim());
    }

    void setRefreshTokenAge(final String tokenAge) {
        setProperty(EventStrings.TOKEN_AGE, tokenAge.trim());
    }

    void setSpeRing(final String speRing) {
        setProperty(EventStrings.SPE_INFO, speRing.trim());
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * Http event adds an event count field
     *
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final String countObject = dispatchMap.get(EventStrings.HTTP_EVENT_COUNT);

        if (countObject == null) {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT, "1");
        } else {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT,
                    Integer.toString(Integer.parseInt(countObject) + 1));
        }

        // If there was a previous entry clear out its fields.
        if (dispatchMap.containsKey(EventStrings.HTTP_RESPONSE_CODE)) {
            dispatchMap.put(EventStrings.HTTP_RESPONSE_CODE, "");
        }

        if (dispatchMap.containsKey(EventStrings.OAUTH_ERROR_CODE)) {
            dispatchMap.put(EventStrings.OAUTH_ERROR_CODE, "");
        }

        if (dispatchMap.containsKey(EventStrings.HTTP_PATH)) {
            dispatchMap.put(EventStrings.HTTP_PATH, "");
        }

        if (dispatchMap.containsKey(EventStrings.REQUEST_ID_HEADER)) {
            dispatchMap.put(EventStrings.REQUEST_ID_HEADER, "");
        }

        if (dispatchMap.containsKey(EventStrings.SERVER_ERROR_CODE)) {
            dispatchMap.remove(EventStrings.SERVER_ERROR_CODE);
        }

        if (dispatchMap.containsKey(EventStrings.SERVER_SUBERROR_CODE)) {
            dispatchMap.remove(EventStrings.SERVER_SUBERROR_CODE);
        }

        if (dispatchMap.containsKey(EventStrings.TOKEN_AGE)) {
            dispatchMap.remove(EventStrings.TOKEN_AGE);
        }

        if (dispatchMap.containsKey(EventStrings.SPE_INFO)) {
            dispatchMap.remove(EventStrings.SPE_INFO);
        }

        final List<Pair<String, String>> eventList = getEventList();
        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;

            if (name.equals(EventStrings.HTTP_RESPONSE_CODE)
                    || name.equals(EventStrings.REQUEST_ID_HEADER)
                    || name.equals(EventStrings.OAUTH_ERROR_CODE)
                    || name.equals(EventStrings.HTTP_PATH)
                    || name.equals(EventStrings.SERVER_ERROR_CODE)
                    || name.equals(EventStrings.SERVER_SUBERROR_CODE)
                    || name.equals(EventStrings.TOKEN_AGE)
                    || name.equals(EventStrings.SPE_INFO)) {
                dispatchMap.put(name, eventPair.second);
            }
        }
    }
}
