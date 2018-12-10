module jaxp.api {
    requires transitive java.xml;

    exports javax.xml;
    exports javax.xml.datatype;
    exports javax.xml.namespace;
    exports javax.xml.parsers;
    exports javax.xml.stream;
    exports javax.xml.stream.events;
    exports javax.xml.stream.util;
    exports javax.xml.transform;
    exports javax.xml.transform.dom;
    exports javax.xml.transform.sax;
    exports javax.xml.transform.stax;
    exports javax.xml.transform.stream;
    exports javax.xml.validation;
    exports javax.xml.xpath;
    exports org.w3c.dom;
    exports org.w3c.dom.bootstrap;
    exports org.w3c.dom.css;
    exports org.w3c.dom.events;
    exports org.w3c.dom.html;
    exports org.w3c.dom.ls;
    exports org.w3c.dom.ranges;
    exports org.w3c.dom.stylesheets;
    exports org.w3c.dom.traversal;
    exports org.w3c.dom.xpath;
    exports org.xml.sax;
    exports org.xml.sax.ext;
    exports org.xml.sax.helpers;

}
