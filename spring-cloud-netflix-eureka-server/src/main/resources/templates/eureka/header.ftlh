<#import "/spring.ftl" as spring />
<nav class="navbar navbar-default" role="navigation">
  <div class="container">
    <div class="navbar-header">
      <a class="navbar-brand" href="<@spring.url dashboardPath/>"><span></span></a>
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
    </div>
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
      <ul class="nav navbar-nav navbar-right">
        <li>
          <a href="<@spring.url dashboardPath/>">Home</a>
        </li>
        <li>
          <a href="<@spring.url dashboardPath/>/lastn">Last 1000 since startup</a>
        </li>
      </ul>
    </div>
  </div>
</nav>


