package org.ccci.idm.user.ldaptive.dao.io;

import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.ldaptive.io.GeneralizedTimeValueTranscoder;

public class ReadableInstantValueTranscoder extends AbstractStringValueTranscoder<ReadableInstant> {
    private static final GeneralizedTimeValueTranscoder TRANSCODER = new GeneralizedTimeValueTranscoder();

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMddHHmmss'Z'").withZoneUTC();

    @Override
    public Class<ReadableInstant> getType() {
        return ReadableInstant.class;
    }

    @Override
    public ReadableInstant decodeStringValue(final String value) {
        return new Instant(TRANSCODER.decodeStringValue(value).toInstant().toEpochMilli());
    }

    @Override
    public String encodeStringValue(final ReadableInstant value) {
        // we use a custom DateTimeFormatter instead of the GeneralizedTimeValueTranscoder because eDirectory does
        // not support fractional seconds
        return FORMATTER.print(value);
    }
}
