---
layout: default
title: Blog
subtitle: Recent Articles
---
 
<ul>
{% for post in site.posts %}
  <li>
  <div class="index-post">
    <a class="index-post-item" href="{{ post.url }}">
      <h2>{{ post.title }}</h2>
    </a> 
  </div>
  <div class="index-post">
    <small class="index-post-item">{{ post.date | date_to_string }}</small>
  </div>
  </li>
{% endfor %}
</ul>