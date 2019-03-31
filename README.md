# pdf-enrichment
Tool to automatically enrich PDFs of research articles with additional information (hyperlinks, metadata, etc.). This tool is under development; for now, as a proof of concept, the tool turns URLs in PDF document into hyperlinks.

## Prerequisites for compilation

pdf-enrichment is programmed in Java, and built using maven. An installation of a reasonably modern JDK (>= 1.8) and of maven is needed. pdf-enrichment makes use of Apache's [PDFBox](https://pdfbox.apache.org/), which is transparently downloaded by maven.

## Compiling

Simply type:

```
mvn package
```

This will produce a JAR in the `target` directory, named `pdfenrichment-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

## Running

Simply type:

```
java -jar target/pdfenrichment-0.0.1-SNAPSHOT-jar-with-dependencies.jar input.pdf output.pdf
```

## License

pdf-enrichment is provided as open-source software under the MIT License. See [LICENSE](LICENSE).

## Contact

https://github.com/PierreSenellart/provsql

Pierre Senellart <pierre@senellart.com>

Bug reports and feature requests are
preferably sent through the *Issues* feature of GitHub.
