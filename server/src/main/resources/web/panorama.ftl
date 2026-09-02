<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title>${file.name} 全景图预览</title>
    <#include "*/commonHeader.ftl">
    <script src="js/base64.min.js" type="text/javascript"></script>
    <link rel="stylesheet" href="pannellum/pannellum.css">
    <script src="pannellum/pannellum.js"></script>
    <style>
        #panorama {
            width: 100%;
            height: 100vh;
            background-color: #000;
        }
    </style>
</head>
<body>
<div id="panorama"></div>

<#if currentUrl?contains("http://") || currentUrl?contains("https://") || currentUrl?contains("file://") || currentUrl?contains("ftp://")>
    <#assign finalUrl="${currentUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${currentUrl}">
</#if>

<script type="text/javascript">
    var rawUrl = '${finalUrl}';
    var kkagent = '${kkagent}';
    var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
    var panoramaUrl = rawUrl;

    // 跨域 / 反代走 getCorsFile，与 picture.ftl 一致
    if (kkagent === 'true' || !rawUrl.startsWith(baseUrl)) {
        panoramaUrl = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(rawUrl)) + "&key=${kkkey}";
    }

    pannellum.viewer('panorama', {
        type: 'equirectangular',
        panorama: panoramaUrl,
        autoLoad: true,
        autoRotate: -2,
        compass: false,
        hfov: 100,
        minHfov: 50,
        maxHfov: 120,
        crossOrigin: 'anonymous'
    });
</script>

<script type="text/javascript">
    /*初始化水印*/
    if (!!window.ActiveXObject || "ActiveXObject" in window) {
    } else {
        initWaterMark();
    }
</script>
</body>
</html>
