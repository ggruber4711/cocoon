<?xml version="1.0"?>
<!--
 *****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * _________________________________________________________________________ *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 ***************************************************************************** 
-->

<!-- XSP Response logicsheet for the Java language -->
<xsl:stylesheet
  version="1.0"
  xmlns:xsp="http://xml.apache.org/xsp"
  xmlns:xsp-response="http://xml.apache.org/xsp/response"

  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
  <xsl:template match="xsp-response:set-header">
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="@name">"<xsl:value-of select="@name"/>"</xsl:when>
        <xsl:when test="name">
          <xsl:call-template name="get-nested-content">
            <xsl:with-param name="content" select="xsp-response:name"/>
          </xsl:call-template>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="value">
      <xsl:choose>
        <xsl:when test="@value">"<xsl:value-of select="@value"/>"</xsl:when>
        <xsl:when test="value">
          <xsl:call-template name="get-nested-content">
            <xsl:with-param name="content" select="xsp-response:value"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>""</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsp:logic>
      XSPResponseHelper.setHeader(this.response, <xsl:value-of select="$name"/>, <xsl:value-of select="$value"/>);
    </xsp:logic>
  </xsl:template>

  <xsl:template match="xsp-response:set-content-type">
    <xsl:variable name="type">
      <xsl:choose>
        <xsl:when test="@type">"<xsl:value-of select="@type"/>"</xsl:when>
        <xsl:when test="type">
          <xsl:call-template name="get-nested-content">
            <xsl:with-param name="content" select="xsp-response:type"/>
          </xsl:call-template>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsp:logic>
      XSPResponseHelper.setContentType(this.response, <xsl:value-of select="$type"/>);
    </xsp:logic>
  </xsl:template>

  <xsl:template name="get-nested-content">
    <xsl:param name="content"/>
    <xsl:choose>
      <xsl:when test="$content/*">
        <xsl:apply-templates select="$content/*"/>
      </xsl:when>
      <xsl:otherwise>"<xsl:value-of select="$content"/>"</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
 
  <xsl:template match="@*|*|text()|processing-instruction()">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()|processing-instruction()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
