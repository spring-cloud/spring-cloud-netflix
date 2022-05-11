<#import "/spring.ftl" as spring />
<nav class="navbar navbar-default navbar-expand-sm navbar-dark bg-dark p-0">
  <div class="container">
    <a class="navbar-brand" href="<@spring.url dashboardPath/>"><span></span></a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse justify-content-end" id="navbarSupportedContent">
      <ul class="navbar-nav">
        <li class="nav-item">
          <a class="nav-link" href="<@spring.url dashboardPath/>">Home</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="<@spring.url dashboardPath/>/lastn">Last 1000 since startup</a>
        </li>
      </ul>
    </div>
  </div>
</nav>


