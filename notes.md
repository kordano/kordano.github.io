---
layout: default
title: Notes
subtitle: Thoughts and Ideas
---
 
<ul>
{% for post in site.posts %}
  <li class="index-posts">
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
