# Pusher Dispatch Integration
The `dispatch-pusher` module provides support for sending events to a
Pusher channel by using the Pusher REST API.

See
[Dispatch Project Setup](http://dispatch.databinder.net/Project+Setup.html)
for how to use Dispatch API integrations in a project.

## Limitations
Since this library uses the REST API, only sending events is currently
supported.  Also, excluding recipients is not currently supported but
could be added fairly easily.  So if you'd like the functionality,
fork and contribute :-)
