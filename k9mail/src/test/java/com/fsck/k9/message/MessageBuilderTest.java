package com.fsck.k9.message;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Application;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Identity;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.message.MessageBuilder.Callback;
import com.fsck.k9.message.quote.InsertableHtmlContent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class MessageBuilderTest extends RobolectricTest {
    private static final String TEST_MESSAGE_TEXT = "soviet message\r\ntext ☭";
    private static final String TEST_ATTACHMENT_TEXT = "text data in attachment";
    private static final String TEST_SUBJECT = "test_subject";
    private static final Address TEST_IDENTITY_ADDRESS = new Address("test@example.org", "tester");
    private static final Address[] TEST_TO = new Address[] {
            new Address("to1@example.org", "recip 1"),
            new Address("to2@example.org", "recip 2")
    };
    private static final Address[] TEST_CC = new Address[] {
            new Address("cc@example.org", "cc recip") };
    private static final Address[] TEST_BCC = new Address[] {
            new Address("bcc@example.org", "bcc recip") };
    private static final String TEST_MESSAGE_ID = "<00000000-0000-007B-0000-0000000000EA@example.org>";
    private static final Date SENT_DATE = new Date(10000000000L);

    private static final String BOUNDARY_1 = "----boundary1";
    private static final String BOUNDARY_2 = "----boundary2";
    private static final String BOUNDARY_3 = "----boundary3";

    private static final String MESSAGE_HEADERS = "" +
            "Date: Sun, 26 Apr 1970 17:46:40 +0000\r\n" +
            "From: tester <test@example.org>\r\n" +
            "To: recip 1 <to1@example.org>,recip 2 <to2@example.org>\r\n" +
            "CC: cc recip <cc@example.org>\r\n" +
            "BCC: bcc recip <bcc@example.org>\r\n" +
            "Subject: test_subject\r\n" +
            "User-Agent: K-9 Mail for Android\r\n" +
            "In-Reply-To: inreplyto\r\n" +
            "References: references\r\n" +
            "Message-ID: " + TEST_MESSAGE_ID + "\r\n" +
            "MIME-Version: 1.0\r\n";

    private static final String MESSAGE_CONTENT = "" +
            "Content-Type: text/plain;\r\n" +
            " charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n" +
            "\r\n" +
            "soviet message\r\n" +
            "text =E2=98=AD";

    private static final String MESSAGE_CONTENT_WITH_ATTACH = "" +
            "Content-Type: multipart/mixed; boundary=\"" + BOUNDARY_1 + "\"\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n" +
            "\r\n" +
            "soviet message\r\n" +
            "text =E2=98=AD\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " name=\"attach.txt\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n" +
            "Content-Disposition: attachment;\r\n" +
            " filename=\"attach.txt\";\r\n" +
            " size=23\r\n" +
            "\r\n" +
            "dGV4dCBkYXRhIGluIGF0dGFjaG1lbnQ=\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "--\r\n";

    private static final String MESSAGE_CONTENT_WITH_LONG_CONTENT_TYPE =
            "Content-Type: multipart/mixed; boundary=\"" + BOUNDARY_1 + "\"\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " title*1*=1234567891123456789212345678931234567894123456789\r\n" +
            " title*2*=5123456789612345678971234567898123456789091234567890;\r\n" +
            " charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n" +
            "\r\n" +
            "soviet message\r\n" +
            "text =E2=98=AD\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " name=\"attach.txt\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n" +
            "Content-Disposition: attachment;\r\n" +
            " filename=\"attach.txt\";\r\n" +
            " size=23\r\n" +
            "\r\n" +
            "dGV4dCBkYXRhIGluIGF0dGFjaG1lbnQ=\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "--\r\n";

    private static final String ATTACHMENT_FILENAME_NON_ASCII = "テスト文書.txt";
    private static final String MESSAGE_CONTENT_WITH_ATTACH_NON_ASCII_FILENAME = "" +
            "Content-Type: multipart/mixed; boundary=\"" + BOUNDARY_1 + "\"\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n" +
            "\r\n" +
            "soviet message\r\n" +
            "text =E2=98=AD\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " name=\"=?UTF-8?B?44OG44K544OI5paH5pu4LnR4dA==?=\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n" +
            "Content-Disposition: attachment;\r\n" +
            " filename=\"=?UTF-8?B?44OG44K544OI5paH5pu4LnR4dA==?=\";\r\n" +
            " size=23\r\n" +
            "\r\n" +
            "dGV4dCBkYXRhIGluIGF0dGFjaG1lbnQ=\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "--\r\n";

    private static final String MESSAGE_CONTENT_WITH_MESSAGE_ATTACH = "" +
            "Content-Type: multipart/mixed; boundary=\"" + BOUNDARY_1 + "\"\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n" +
            "\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: text/plain;\r\n" +
            " charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n" +
            "\r\n" +
            "soviet message\r\n" +
            "text =E2=98=AD\r\n" +
            "--" + BOUNDARY_1 + "\r\n" +
            "Content-Type: message/rfc822;\r\n" +
            " name=\"attach.txt\"\r\n" +
            "Content-Disposition: attachment;\r\n" +
            " filename=\"attach.txt\";\r\n" +
            " size=23\r\n" +
            "\r\n" +
            "text data in attachment" +
            "\r\n" +
            "--" + BOUNDARY_1 + "--\r\n";


    private Application context;
    private MessageIdGenerator messageIdGenerator;
    private BoundaryGenerator boundaryGenerator;
    private Callback callback;


    @Before
    public void setUp() throws Exception {
        messageIdGenerator = mock(MessageIdGenerator.class);
        when(messageIdGenerator.generateMessageId(any(Message.class))).thenReturn(TEST_MESSAGE_ID);

        boundaryGenerator = mock(BoundaryGenerator.class);
        when(boundaryGenerator.generateBoundary()).thenReturn(BOUNDARY_1, BOUNDARY_2, BOUNDARY_3);

        callback = mock(Callback.class);
        context =  RuntimeEnvironment.application;
    }

    @Test
    public void build_shouldSucceed() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals("text/plain", message.getMimeType());
        assertEquals(TEST_SUBJECT, message.getSubject());
        assertEquals(TEST_IDENTITY_ADDRESS, message.getFrom()[0]);
        assertArrayEquals(TEST_TO, message.getRecipients(RecipientType.TO));
        assertArrayEquals(TEST_CC, message.getRecipients(RecipientType.CC));
        assertArrayEquals(TEST_BCC, message.getRecipients(RecipientType.BCC));
        assertEquals(MESSAGE_HEADERS + MESSAGE_CONTENT, getMessageContents(message));
    }

    @Test
    public void build_withAttachment_shouldSucceed() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();
        Attachment attachment = createAttachmentWithContent(
                "text/plain", "attach.txt", TEST_ATTACHMENT_TEXT);
        messageBuilder.setAttachments(Collections.singletonList(attachment));

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals(MESSAGE_HEADERS + MESSAGE_CONTENT_WITH_ATTACH, getMessageContents(message));
    }

    @Ignore("RFC2231/2184 not implemented") @Test
    public void build_withAttachment_longContentType_shouldSucceed() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();
        Attachment attachment = createAttachmentWithContent(
                "text/plain;title=1234567891123456789212345678931234567894123456789" +
                        "5123456789612345678971234567898123456789091234567890",
                "attach.txt", TEST_ATTACHMENT_TEXT);
        messageBuilder.setAttachments(Collections.singletonList(attachment));

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals(MESSAGE_HEADERS + MESSAGE_CONTENT_WITH_LONG_CONTENT_TYPE,
                getMessageContents(message));
    }

    @Test
    public void build_withAttachment_nonAscii_shouldSucceed() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();
        Attachment attachment = createAttachmentWithContent(
                "text/plain", ATTACHMENT_FILENAME_NON_ASCII, TEST_ATTACHMENT_TEXT);
        messageBuilder.setAttachments(Collections.singletonList(attachment));

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals(MESSAGE_HEADERS + MESSAGE_CONTENT_WITH_ATTACH_NON_ASCII_FILENAME,
                getMessageContents(message));
    }

    @Test
    public void build_usingHtmlFormat_shouldUseMultipartAlternativeInCorrectOrder() {
        MessageBuilder messageBuilder = createHtmlMessageBuilder();

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals(MimeMultipart.class, message.getBody().getClass());
        assertEquals("multipart/alternative", ((MimeMultipart) message.getBody()).getMimeType());
        List<BodyPart> parts =  ((MimeMultipart) message.getBody()).getBodyParts();
        //RFC 2046 - 5.1.4. - Best type is last displayable
        assertEquals("text/plain", parts.get(0).getMimeType());
        assertEquals("text/html", parts.get(1).getMimeType());
    }

    @Test
    public void build_withMessageAttachment_shouldAttachAsMessageRfc822() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();
        Attachment attachment = createAttachmentWithContent(
                "message/rfc822", "attach.txt", TEST_ATTACHMENT_TEXT);
        messageBuilder.setAttachments(Collections.singletonList(attachment));

        messageBuilder.buildAsync(callback);

        MimeMessage message = getMessageFromCallback();
        assertEquals(MESSAGE_HEADERS + MESSAGE_CONTENT_WITH_MESSAGE_ATTACH,
                getMessageContents(message));
    }

    @Test
    public void build_detachAndReattach_shouldSucceed() throws MessagingException {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();
        Callback anotherCallback = mock(Callback.class);

        Robolectric.getBackgroundThreadScheduler().pause();
        messageBuilder.buildAsync(callback);
        messageBuilder.detachCallback();
        Robolectric.getBackgroundThreadScheduler().unPause();
        messageBuilder.reattachCallback(anotherCallback);

        verifyNoMoreInteractions(callback);
        verify(anotherCallback).onMessageBuildSuccess(any(MimeMessage.class), eq(false));
        verifyNoMoreInteractions(anotherCallback);
    }

    @Test
    public void buildWithException_shouldThrow() throws MessagingException {
        MessageBuilder messageBuilder = new SimpleMessageBuilder(context, messageIdGenerator, boundaryGenerator) {
            @Override
            protected void buildMessageInternal() {
                queueMessageBuildException(new MessagingException("expected error"));
            }
        };

        messageBuilder.buildAsync(callback);

        verify(callback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void buildWithException_detachAndReattach_shouldThrow() throws MessagingException {
        Callback anotherCallback = mock(Callback.class);
        MessageBuilder messageBuilder = new SimpleMessageBuilder(context, messageIdGenerator, boundaryGenerator) {
            @Override
            protected void buildMessageInternal() {
                queueMessageBuildException(new MessagingException("expected error"));
            }
        };

        Robolectric.getBackgroundThreadScheduler().pause();
        messageBuilder.buildAsync(callback);
        messageBuilder.detachCallback();
        Robolectric.getBackgroundThreadScheduler().unPause();
        messageBuilder.reattachCallback(anotherCallback);

        verifyNoMoreInteractions(callback);
        verify(anotherCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(anotherCallback);
    }

    private MimeMessage getMessageFromCallback() {
        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(callback).onMessageBuildSuccess(mimeMessageCaptor.capture(), eq(false));
        verifyNoMoreInteractions(callback);

        return mimeMessageCaptor.getValue();
    }

    private String getMessageContents(MimeMessage message) throws IOException, MessagingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        return outputStream.toString();
    }

    private Attachment createAttachmentWithContent(String mimeType, String filename, String content) throws Exception {
        byte[] bytes = content.getBytes();
        File tempFile = File.createTempFile("pre", ".tmp");
        tempFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        fileOutputStream.write(bytes);
        fileOutputStream.close();

        return Attachment.createAttachment(null, 0, mimeType, true)
                .deriveWithMetadataLoaded(mimeType, filename, bytes.length)
                .deriveWithLoadComplete(tempFile.getAbsolutePath());
    }

    private MessageBuilder createSimpleMessageBuilder() {
        Identity identity = createIdentity();
        return new SimpleMessageBuilder(context, messageIdGenerator, boundaryGenerator)
                .setSubject(TEST_SUBJECT)
                .setSentDate(SENT_DATE)
                .setHideTimeZone(true)
                .setTo(Arrays.asList(TEST_TO))
                .setCc(Arrays.asList(TEST_CC))
                .setBcc(Arrays.asList(TEST_BCC))
                .setInReplyTo("inreplyto")
                .setReferences("references")
                .setRequestReadReceipt(false)
                .setIdentity(identity)
                .setMessageFormat(SimpleMessageFormat.TEXT)
                .setText(TEST_MESSAGE_TEXT)
                .setAttachments(new ArrayList<Attachment>())
                .setSignature("signature")
                .setQuoteStyle(QuoteStyle.PREFIX)
                .setQuotedTextMode(QuotedTextMode.NONE)
                .setQuotedText("quoted text")
                .setQuotedHtmlContent(new InsertableHtmlContent())
                .setReplyAfterQuote(false)
                .setSignatureBeforeQuotedText(false)
                .setIdentityChanged(false)
                .setSignatureChanged(false)
                .setCursorPosition(0)
                .setMessageReference(null)
                .setDraft(false);
    }

    private MessageBuilder createHtmlMessageBuilder() {
        return createSimpleMessageBuilder().setMessageFormat(SimpleMessageFormat.HTML);
    }

    private Identity createIdentity() {
        Identity identity = new Identity();
        identity.setName(TEST_IDENTITY_ADDRESS.getPersonal());
        identity.setEmail(TEST_IDENTITY_ADDRESS.getAddress());
        identity.setDescription("test identity");
        identity.setSignatureUse(false);
        return identity;
    }
}
