package org.dromara.easyos.exception;

public class EasyOsException extends RuntimeException {

    public EasyOsException(String message) {
        super(message);
    }

    public EasyOsException(String message, Throwable cause) {
        super(message, cause);
    }

    public static EasyOsException sqlPluginMissing() {
        return new EasyOsException(
                "OpenSearch SQL plugin is required.\n"
                        + "Check: bin/opensearch-plugin list  (expect opensearch-sql)\n"
                        + "Install (minimal distro): bin/opensearch-plugin install opensearch-sql\n"
                        + "Then restart the node.\n"
                        + "Docs: https://docs.opensearch.org/latest/install-and-configure/plugins/");
    }
}
