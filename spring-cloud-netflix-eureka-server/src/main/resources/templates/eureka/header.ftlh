<#import "/spring.ftl" as spring />
<nav class="navbar navbar-expand-md navbar-dark" role="navigation">
  <div class="container">
    <a class="navbar-brand my-auto" href="<@spring.url dashboardPath/>"><span></span></a>
    <button type="button" class="navbar-toggler" data-bs-toggle="collapse" data-bs-target="#navbarContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarContent">
      <ul class="navbar-nav ms-auto">
        <li class="nav-item">
          <a class="nav-link px-2" href="<@spring.url dashboardPath/>">Home</a>
        </li>
        <li class="nav-item">
          <a class="nav-link px-2" href="<@spring.url dashboardPath/>/lastn">Last 1000 since startup</a>
        </li>
      </ul>
    </div>
  </div>
</nav>


