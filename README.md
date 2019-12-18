# pdf-enrichment
Tool to automatically enrich PDFs of research articles with additional information (hyperlinks, metadata, etc.). This tool is under development; for now, as a proof of concept, the tool turns URLs in PDF documents into hyperlinks, and turns the title of article in reference entries into a link to their open-access version if any is available (using [Dissemin](https://dissem.in/) to identify such open-access versions).

## Prerequisites for compilation

pdf-enrichment is programmed in Java, and built using maven. An installation of a reasonably modern JDK (>= 1.8) and of maven is needed.

## Libraries used

pdf-enrichment makes use of Apache's [PDFBox](https://pdfbox.apache.org/) and Google's [JSON.simple](https://code.google.com/archive/p/json-simple/), which are transparently downloaded by maven.

pdf-enrichment also uses [Grobid](https://github.com/kermitt2/grobid/), which is included in the distribution (in the [lib/](lib/) folder). 

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

[Grobid](https://github.com/kermitt2/grobid/), distributed in the [lib/](lib/) folder, is released under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) open-source license.  

## Contact

https://github.com/PierreSenellart/pdf-enrichment

Pierre Senellart <pierre@senellart.com>

Bug reports and feature requests are
preferably sent through the *Issues* feature of GitHub.
