jQuery(document).ready(
function() {
  var customGovernancePluginUrl = SailPoint.CONTEXT_PATH  + '/plugins/pluginPage.jsf?pn=custom_governance_plugin';
  jQuery("ul.navbar-right li:first").before(
    '<li class="dropdown"><a href="'+ customGovernancePluginUrl + '" tabindex="0" role="menuitem" title="Custom Governance Plugin">'
  + '<i role="presenation" class="fa fa-thumbs-o-up fa-lg example"></i>'
  + '</a></li>');
});