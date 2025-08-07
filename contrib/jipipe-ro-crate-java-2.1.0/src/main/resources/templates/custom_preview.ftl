<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${crateModel.crate.name!}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        :root {
            --primary: #1a73e8;
            --background: #f9f9f9;
            --text: #333;
            --card-bg: #fff;
            --border: #e0e0e0;
            --font: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            margin: 0;
            padding: 0;
            font-family: var(--font);
            background-color: var(--background);
            color: var(--text);
            line-height: 1.6;
        }

        header, main {
            max-width: 1000px;
            margin: 0 auto;
            padding: 2rem;
        }

        header {
            background-color: var(--card-bg);
            border-bottom: 1px solid var(--border);
            border-radius: 0 0 8px 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        header h1 {
            margin-top: 0;
            font-size: 2rem;
            color: var(--primary);
        }

        p, li {
            margin: 0.5rem 0;
        }

        section {
            margin-top: 2rem;
            background-color: var(--card-bg);
            padding: 1.5rem;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            border: 1px solid var(--border);
        }

        h2 {
            margin-top: 0;
            color: var(--primary);
        }

        ul {
            list-style: none;
            padding: 0;
        }

        li {
            padding: 0.5rem 0;
            border-bottom: 1px solid var(--border);
        }

        li:last-child {
            border-bottom: none;
        }

        a {
            color: var(--primary);
            text-decoration: none;
        }

        a:hover {
            text-decoration: underline;
        }

        @media (max-width: 600px) {
            body {
                font-size: 0.95rem;
            }

            header, main {
                padding: 1rem;
            }

            h1 {
                font-size: 1.5rem;
            }

            h2 {
                font-size: 1.25rem;
            }
        }
    </style>
</head>
<body>

<header>
    <h1>${crateModel.crate.name!}</h1>

    <#if crateModel.crate.description??>
        <p><strong>Description:</strong> ${crateModel.crate.description}</p>
    </#if>

    <#if crateModel.crate.license??>
        <p><strong>License:</strong>
            <#if crateModel.crate.license?starts_with("http")>
                <a href="${crateModel.crate.license}">${crateModel.crate.license}</a>
            <#else>
                ${crateModel.crate.license}
            </#if>
        </p>
    </#if>

    <#if crateModel.crate.datePublished??>
        <p><strong>Date Published:</strong> ${crateModel.crate.datePublished}</p>
    </#if>
</header>

<main>

    <#if crateModel.crate.hasPart?? && crateModel.crate.hasPart?size gt 0>
    <section>
        <h2>Parts</h2>
        <ul>
            <#list crateModel.crate.hasPart as part>
                <li>
                    <strong>${part.name!}</strong>
                    <#if part.id??>
                        - <a href="${part.id}">${part.id}</a>
                    </#if>
                </li>
            </#list>
        </ul>
    </section>
    </#if>

    <#if crateModel.datasets?? && crateModel.datasets?size gt 0>
    <section>
        <h2>Datasets</h2>
        <ul>
            <#list crateModel.datasets as dataset>
                <li>
                    <strong>${dataset.name!}</strong>
                    <#if dataset.id??>
                        - <a href="${dataset.id}">${dataset.id}</a>
                    </#if>
                    <#if dataset.description??>
                        <br>${dataset.description}
                    </#if>
                </li>
            </#list>
        </ul>
    </section>
    </#if>

    <#if crateModel.files?? && crateModel.files?size gt 0>
    <section>
        <h2>Files</h2>
        <ul>
            <#list crateModel.files as file>
                <li>
                    <strong>${file.name!}</strong>
                    <#if file.id??>
                        - <a href="${file.id}">${file.id}</a>
                    </#if>
                    <#if file.description??>
                        <br>${file.description}
                    </#if>
                    <#if file.contentSize??>
                        <br><em>Size:</em> ${file.contentSize}
                    </#if>
                    <#if file.encodingFormat??>
                        <br><em>Format:</em> ${file.encodingFormat}
                    </#if>
                </li>
            </#list>
        </ul>
    </section>
    </#if>

</main>

<footer>
    <p style="text-align: center; margin-top: 2rem; font-size: 0.8rem;">
        This human-readable preview has been created using ro-crate-java.
    </p>
</footer>

</body>
</html>