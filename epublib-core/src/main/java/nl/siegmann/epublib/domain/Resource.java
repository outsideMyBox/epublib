package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.StringUtil;
import nl.siegmann.epublib.util.commons.io.XmlStreamReader;

import java.io.*;

/**
 * Represents a resource that is part of the epub. A resource can be a html file, image, xml, etc.
 * @author paul
 */
public class Resource implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1043946707835004037L;
    private String id;
    private String title;
    private String href;
    private MediaTypeProperty mediaTypeProperty;
    private String inputEncoding = Constants.CHARACTER_ENCODING;
    private byte[] data;

    private String fileName;
    private long cachedSize;

    /**
     * Creates an empty Resource with the given href.
     * <p>
     * Assumes that if the data is of a text type (html/css/etc) then the encoding will be UTF-8
     * @param href The location of the resource within the epub. Example: "chapter1.html".
     */
    public Resource(String href) {
        this(null, new byte[0], href, MediatypeService.determineMediaType(href));
    }

    /**
     * Creates a Resource with the given data and MediaType. The href will be automatically generated.
     * <p>
     * Assumes that if the data is of a text type (html/css/etc) then the encoding will be UTF-8
     * @param data              The Resource's contents
     * @param mediaTypeProperty The MediaType of the Resource
     */
    public Resource(byte[] data, MediaTypeProperty mediaTypeProperty) {
        this(null, data, null, mediaTypeProperty);
    }

    /**
     * Creates a resource with the given data at the specified href. The MediaType will be determined based on the href
     * extension.
     * <p>
     * Assumes that if the data is of a text type (html/css/etc) then the encoding will be UTF-8
     * @param data The Resource's contents
     * @param href The location of the resource within the epub. Example: "chapter1.html".
     * @see nl.siegmann.epublib.service.MediatypeService.determineMediaType(String)
     */
    public Resource(byte[] data, String href) {
        this(null, data, href, MediatypeService.determineMediaType(href), Constants.CHARACTER_ENCODING);
    }

    /**
     * Creates a resource with the data from the given Reader at the specified href. The MediaType will be determined
     * based on the href extension.
     * @param in   The Resource's contents
     * @param href The location of the resource within the epub. Example: "cover.jpg".
     * @see nl.siegmann.epublib.service.MediatypeService.determineMediaType(String)
     */
    public Resource(Reader in, String href) throws IOException {
        this(null, IOUtil.toByteArray(in, Constants.CHARACTER_ENCODING), href,
                MediatypeService.determineMediaType(href), Constants.CHARACTER_ENCODING);
    }

    /**
     * Creates a resource with the data from the given InputStream at the specified href. The MediaType will be
     * determined based on the href extension.
     * @param in   The Resource's contents
     * @param href The location of the resource within the epub. Example: "cover.jpg".
     * @see nl.siegmann.epublib.service.MediatypeService.determineMediaType(String)
     * <p>
     * Assumes that if the data is of a text type (html/css/etc) then the encoding will be UTF-8
     * <p>
     * It is recommended to us the
     * @see nl.siegmann.epublib.domain.Resource.Resource(Reader, String) method for creating textual (html/css/etc)
     * resources to prevent encoding problems. Use this method only for binary Resources like images, fonts, etc.
     */
    public Resource(InputStream in, String href) throws IOException {
        this(null, IOUtil.toByteArray(in), href, MediatypeService.determineMediaType(href));
    }

    public Resource(String id, InputStream in, String href) throws IOException {
        this(id, IOUtil.toByteArray(in), href, MediatypeService.determineMediaType(href));
    }

    /**
     * Creates a Resource that tries to load the data, but falls back to lazy loading.
     * <p>
     * If the size of the resource is known ahead of time we can use that to allocate a matching byte[]. If this
     * succeeds we can safely load the data.
     * <p>
     * If it fails we leave the data null for now and it will be lazy-loaded when it is accessed.
     * @param in
     * @param fileName
     * @param length
     * @param href
     * @throws IOException
     */
    public Resource(InputStream in, String fileName, int length, String href) throws IOException {
        this(null, IOUtil.toByteArray(in, length), href, MediatypeService.determineMediaType(href));
        this.fileName = fileName;
        this.cachedSize = length;
    }

    /**
     * Creates a Lazy resource, by not actually loading the data for this entry.
     * <p>
     * The data will be loaded on the first call to getData()
     * @param fileName the fileName for the epub we're created from.
     * @param size     the size of this resource.
     * @param href     The resource's href within the epub.
     */
    public Resource(String fileName, long size, String href) {
        this(null, null, href, MediatypeService.determineMediaType(href));
        this.fileName = fileName;
        this.cachedSize = size;
    }

    /**
     * Creates a resource with the given id, data, mediatype at the specified href. Assumes that if the data is of a
     * text type (html/css/etc) then the encoding will be UTF-8
     * @param id                The id of the Resource. Internal use only. Will be auto-generated if it has a
     *                          null-value.
     * @param data              The Resource's contents
     * @param href              The location of the resource within the epub. Example: "chapter1.html".
     * @param mediaTypeProperty The resources MediaType
     */
    public Resource(String id, byte[] data, String href, MediaTypeProperty mediaTypeProperty) {
        this(id, data, href, mediaTypeProperty, Constants.CHARACTER_ENCODING);
    }

    /**
     * Creates a resource with the given id, data, mediatype at the specified href. If the data is of a text type
     * (html/css/etc) then it will use the given inputEncoding.
     * @param id                The id of the Resource. Internal use only. Will be auto-generated if it has a
     *                          null-value.
     * @param data              The Resource's contents
     * @param href              The location of the resource within the epub. Example: "chapter1.html".
     * @param mediaTypeProperty The resources MediaType
     * @param inputEncoding     If the data is of a text type (html/css/etc) then it will use the given inputEncoding.
     */
    public Resource(String id, byte[] data, String href, MediaTypeProperty mediaTypeProperty, String inputEncoding) {
        this.id = id;
        this.href = href;
        this.mediaTypeProperty = mediaTypeProperty;
        this.inputEncoding = inputEncoding;
        this.data = data;
    }

    /**
     * Gets the contents of the Resource as an InputStream.
     * @return The contents of the Resource.
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getData());
    }

    /**
     * The contents of the resource as a byte[]
     * <p>
     * If this resource was lazy-loaded and the data was not yet loaded, it will be loaded into memory at this point.
     * This included opening the zip file, so expect a first load to be slow.
     * @return The contents of the resource
     */
    public byte[] getData() throws IOException {

        if (data == null) {
            FileInputStream inputStream = new FileInputStream(fileName);
            byte[] data = IOUtil.toByteArray(inputStream);
            inputStream.close();
            return data;
        }

        return data;
    }

    /**
     * Tells this resource to release its cached data.
     * <p>
     * If this resource was not lazy-loaded, this is a no-op.
     */
    public void close() {
        if (this.fileName != null) {
            this.data = null;
        }
    }

    /**
     * Sets the data of the Resource. If the data is a of a different type then the original data then make sure to
     * change the MediaType.
     * @param data
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Returns if the data for this resource has been loaded into memory.
     * @return true if data was loaded.
     */
    public boolean isInitialized() {
        return data != null;
    }

    /**
     * Returns the size of this resource in bytes.
     * @return the size.
     */
    public long getSize() {
        if (data != null) {
            return data.length;
        }

        return cachedSize;
    }

    /**
     * If the title is found by scanning the underlying html document then it is cached here.
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the Resource's id: Make sure it is unique and a valid identifier.
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The resources Id.
     * <p>
     * Must be both unique within all the resources of this book and a valid identifier.
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * The location of the resource within the contents folder of the epub file.
     * <p>
     * Example:<br/> images/cover.jpg<br/> content/chapter1.xhtml<br/>
     * @return
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the Resource's href.
     * @param href
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * The character encoding of the resource. Is allowed to be null for non-text resources like images.
     * @return
     */
    public String getInputEncoding() {
        return inputEncoding;
    }

    /**
     * Sets the Resource's input character encoding.
     * @param encoding
     */
    public void setInputEncoding(String encoding) {
        this.inputEncoding = encoding;
    }

    /**
     * Gets the contents of the Resource as Reader.
     * <p>
     * Does all sorts of smart things (courtesy of apache commons io XMLStreamREader) to handle encodings, byte order
     * markers, etc.
     * @return
     * @throws IOException
     */
    public Reader getReader() throws IOException {
        return new XmlStreamReader(new ByteArrayInputStream(getData()), getInputEncoding());
    }

    /**
     * Gets the hashCode of the Resource's href.
     */
    public int hashCode() {
        return href.hashCode();
    }

    /**
     * Checks to see of the given resourceObject is a resource and whether its href is equal to this one.
     */
    public boolean equals(Object resourceObject) {
        if (!(resourceObject instanceof Resource)) {
            return false;
        }
        return href.equals(((Resource) resourceObject).getHref());
    }

    /**
     * This resource's mediaType.
     * @return
     */
    public MediaTypeProperty getMediaTypeProperty() {
        return mediaTypeProperty;
    }

    public void setMediaTypeProperty(MediaTypeProperty mediaTypeProperty) {
        this.mediaTypeProperty = mediaTypeProperty;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String toString() {
        return StringUtil.toString("id", id,
                "title", title,
                "encoding", inputEncoding,
                "mediaType", mediaTypeProperty,
                "href", href,
                "size", (data == null ? 0 : data.length));
    }
}
