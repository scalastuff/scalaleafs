---
title: CSS Selectors
layout: default
---

A CSS selector is string that can be used to match elements in a transformation. For example, the following expression is a transformation that replaces all `<h1>` elements by `<h2>` elements:

{% highlight scala %}
{% endhighlight %}


<table cell-spacing="16px">
<tr><td><code>#id</code></td><td>Selects elements with id <code>id</code></td></tr>
<tr><td><code>.selected</code></td><td>Selects elements with class <code>selected</code>.</td></tr>
<tr><td><code>.useful.selected</code></td><td>Selects elements that have both the <code>useful</code> and <code>selected</code> class.</td></tr>
<tr><td><code>input</code></td><td>Selects all <code>input</code> elements.</td></tr>
<tr><td><code>div.selected input</code></td><td>Selects all <code>input</code> elements within div's that have the <code>selected</code> class.</td></tr>
<tr><td><code>[selected]</code></td><td>Selects all elements that have an attribute named <code>selected</code>, with or without a value.</td></tr>
<tr><td><code>[name='street']</code></td><td>Selects all elements where the name attribute equals <code>street</code>.</td></tr>
<tr><td><code>[name~='street']</code></td><td>Selects all elements where the value attribute contains a value <code>street</code>.</td></tr>
<tr><td><code>input[value^='a']</code></td><td>Selects all <code>input</code> elements with a value attribute starting with an <code>a</code>.</td></tr>
<tr><td><code>input[value$='a']</code></td><td>Selects all <code>input</code> elements with a value attribute ending with an <code>a</code>.</td></tr>
<tr><td><code>input[value|='sub']</code></td><td>Selects all <code>input</code> elements with a value <code>sub</code> or starting with <code>sub-</code>.</td></tr>
<tr><td><code>input[value*='sub']</code></td><td>Selects all <code>input</code> elements with a value attribute containing <code>sub</code>.</td></tr>
</table>
