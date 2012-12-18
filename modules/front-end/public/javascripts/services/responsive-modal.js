/**
 * Exposes Jordan Schroter's Bootstrap modal (https://github.com/jschr/bootstrap-modal)
 * in a way that guarantees that bootstrap's native modal loads first. This is necessary
 * because if Schroter's library loads first it behaves improperly.
 */
define(["bootstrap/bootstrap-modal", "bootstrap-modal/bootstrap-modalmanager", "bootstrap-modal/bootstrap-modal"],
function() {
  return {};
});