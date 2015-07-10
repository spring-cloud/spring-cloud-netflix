var defaultBomVersion = "Angel.SR3";
var boms = {
  "1.0.0.RELEASE": "Angel.RELEASE",
  "1.0.1.RELEASE": "Angel.SR1",
  "1.0.2.RELEASE": "Angel.SR2",
  "1.0.3.RELEASE": "Angel.SR3",
  "1.0.4.BUILD-SNAPSHOT": "Angel.BUILD-SNAPSHOT",
  "1.1.0.BUILD-SNAPSHOT": "Brixton.BUILD-SNAPSHOT"
};
function findBom(version) {
    if (boms[version]) return boms[version];
    return defaultBomVersion;
}
