---
layout: default
---
{% for post in site.posts limit:3 %}
  {% include post_preview.html %}
{% endfor %}

# How does it work?

{% for page in site.pages %}
[{{ page.title }}]({{page.url}})
{% endfor %}

Enter Text in Markdown format.