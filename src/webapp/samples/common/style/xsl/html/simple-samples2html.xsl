<?xml version="1.0"?>
<!-- Author: Nicola Ken Barozzi "nicolaken@apache.org" -->
<!-- Author: Vadim Gritsenko "vgritsenko@apache.org" -->
<!-- Author: Christian Haul "haul@apache.org" -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:param name="contextPath" select="string('/cocoon')"/>

 <xsl:template match="/">
  <html>
   <head>
    <title>Apache Cocoon @version@</title>
    <link rel="SHORTCUT ICON" href="favicon.ico"/>
   </head>
   <body bgcolor="#ffffff" link="#0086b2" vlink="#00698c" alink="#743e75">
    <table border="0" cellspacing="2" cellpadding="2" align="center" width="100%">
     <tr>
      <td width="*"><font face="arial,helvetica,sanserif" color="#000000">The Apache Software Foundation is proud to present...</font></td>
      <td width="40%" align="center"><img border="0" src="/images/cocoon.gif"/></td>
      <td width="30%" align="center"><font face="arial,helvetica,sanserif" color="#000000"><b>version @version@</b></font></td>
     </tr>
     <tr>
       <table bgcolor="#000000" border="0" cellspacing="2" cellpadding="2" align="center" width="100%">
         <tr>
            <td width="60%" bgcolor="#0086b2">
              <font size="+1" face="arial,helvetica,sanserif" color="#ffffff"><xsl:value-of select="samples/@name"/></font>
            </td>
            <td align="right" bgcolor="#0086b2"><i>orthogonal views: </i></td>
            <td nowrap="nowrap" bgcolor="#ffffff"><a href="?cocoon-view=content"><i>content</i></a></td>
            <td nowrap="nowrap" bgcolor="#ffffff"><a href="?cocoon-view=pretty-content"><i>pretty content</i></a></td>
            <td nowrap="nowrap" bgcolor="#ffffff"><a href="?cocoon-view=links"><i>links</i></a></td>
          </tr>
       </table>
     </tr>
    </table>

    <xsl:apply-templates select="samples"/>

    <p align="center">
     <font size="-1">
      Copyright &#169; @year@ <a href="http://www.apache.org/">The Apache Software Foundation</a>.<br/>
      All rights reserved.
     </font>
    </p>
   </body>
  </html>
 </xsl:template>

 <xsl:template match="samples">
  <xsl:variable name="gc" select="4"/><!-- group correction -->
  <xsl:variable name="all-groups" select="$gc * count(group)"/>
  <xsl:variable name="all-samples" select="count(group/sample)+count(group/note)+$all-groups"/>
  <xsl:variable name="half-samples" select="round($all-samples div 2)"/>
  <xsl:variable name="half-possibilities">
    <xsl:choose>
      <xsl:when test="count(group) = 1">1 </xsl:when><!-- single group sample.xml -->
      <xsl:otherwise>
        <xsl:for-each select="group">
          <xsl:if test="position() &lt; last() and position() &gt;= 1">
            <xsl:variable name="group-position" select="position()"/>
            <xsl:variable name="prev-sample" select="count(../group[position() &lt;= $group-position - 1]/sample) + count(../group[position() &lt;= $group-position - 1]/note) + position() * $gc - $gc"/>
            <xsl:variable name="curr-sample" select="count(../group[position() &lt;= $group-position]/sample) + count(../group[position() &lt;= $group-position]/note) + position() * $gc"/>
            <xsl:variable name="next-sample" select="count(../group[position() &lt;= $group-position + 1]/sample) + count(../group[position() &lt;= $group-position + 1]/note) + position() * $gc + $gc"/>
            <xsl:variable name="prev-deviation">
              <xsl:choose>
                <xsl:when test="$prev-sample &gt; $half-samples">
                  <xsl:value-of select="$prev-sample - $half-samples"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$half-samples - $prev-sample"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="curr-deviation">
              <xsl:choose>
                <xsl:when test="$curr-sample &gt; $half-samples">
                  <xsl:value-of select="$curr-sample - $half-samples"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$half-samples - $curr-sample"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="next-deviation">
              <xsl:choose>
                <xsl:when test="$next-sample &gt; $half-samples">
                  <xsl:value-of select="$next-sample - $half-samples"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$half-samples - $next-sample"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:if test="$prev-deviation &gt;= $curr-deviation and $curr-deviation &lt;= $next-deviation">
              <xsl:value-of select="$group-position"/><xsl:text> </xsl:text>
            </xsl:if>
          </xsl:if>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="half">
    <xsl:value-of select="substring-before($half-possibilities, ' ')"/>
  </xsl:variable>

  <table width="100%">
   <tr>
    <td width="50%" valign="top">
     <xsl:for-each select="group">  
      <xsl:variable name="group-position" select="position()"/>

      <xsl:choose>
       <xsl:when test="$group-position &lt;= $half">
       <table border="0" bgcolor="#000000" cellpadding="0" cellspacing="0" width="97%">
        <tbody>
         <tr>
          <td>

        <table bgcolor="#000000" border="0" cellspacing="2" cellpadding="2" align="center" width="100%">
         <tr>
          <td bgcolor="#0086b2" width="100%" align="left">
           <font size="+1" face="arial,helvetica,sanserif" color="#ffffff"><xsl:value-of select="@name"/></font>
          </td>
         </tr>
         <tr>
          <td width="100%" bgcolor="#ffffff" align="left">
           <table bgcolor="#ffffff" border="0" cellspacing="0" cellpadding="2"  width="100%" align="center">
            <xsl:apply-templates/>
           </table>
          </td>
         </tr>
        </table>
        
          </td>
         </tr>        
         </tbody>
        </table>
        
        <br/>
       </xsl:when>
       <xsl:otherwise></xsl:otherwise>
      </xsl:choose>
     </xsl:for-each>
    </td>
    <td valign="top">
     <xsl:for-each select="group">  <!-- [position()<=$half] -->
      <xsl:variable name="group-position" select="position()"/>

      <xsl:choose>
       <xsl:when test="$group-position &gt; $half">
       <table border="0" bgcolor="#000000" cellpadding="0" cellspacing="0" width="97%">
        <tbody>
         <tr>
          <td>       
        <table bgcolor="#000000" border="0" cellspacing="2" cellpadding="2" align="center" width="100%">
         <tr>
          <td bgcolor="#0086b2" width="100%" align="left">
           <font size="+1" face="arial,helvetica,sanserif" color="#ffffff"><xsl:value-of select="@name"/></font>
          </td>
         </tr>
         <tr>
          <td width="100%" bgcolor="#ffffff" align="left">
           <table bgcolor="#ffffff" border="0" cellspacing="0" cellpadding="2"  width="100%" align="center">
            <xsl:apply-templates/>
           </table>
          </td>
         </tr>
        </table>
         </td>
         </tr>        
         </tbody>
        </table>
        <br/>
       </xsl:when>
       <xsl:otherwise></xsl:otherwise>
      </xsl:choose>
     </xsl:for-each>
    </td>
   </tr>
  </table>
 </xsl:template>
 
 <xsl:template match="sample">
   <xsl:variable name="link">
     <xsl:choose>
       <xsl:when test="starts-with(@href,'/')">
         <xsl:value-of select="concat($contextPath,@href)"/>
       </xsl:when>
       <xsl:otherwise>
         <xsl:value-of select="@href"/>
       </xsl:otherwise>
     </xsl:choose>
   </xsl:variable>
   <tr>
     <td width="100%" bgcolor="#ffffff" align="left">
       <font size="+0" face="arial,helvetica,sanserif" color="#000000">    
       <a href="{$link}"><xsl:value-of select="@name"/></a><xsl:text> - </xsl:text>
       <xsl:value-of select="."/>
     </font>
   </td>
  </tr>
 </xsl:template>
 
 <xsl:template match="note">
  <tr>
   <td width="100%" bgcolor="#ffffff" align="left">
    <font size="+0" face="arial,helvetica,sanserif" color="#000000">    
      <xsl:value-of select="."/>
    </font>
   </td>
  </tr>
 </xsl:template>

</xsl:stylesheet>
