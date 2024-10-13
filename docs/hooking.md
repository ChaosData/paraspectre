## Hooking With paraspectre

paraspectre is based around hooking Java methods with (J)Ruby. But before any
of your Ruby code will run, a hook invoking it must be configured.

## Registering Hooks

paraspectre hooks take the form of a JSON configuration declaring a number of
class and method matching selectors. Each matcher object contains the following
fields:

 * `pkg`: A string specifying the Android application package (omitted when
   using a `hooks/com.pkg.name.json` file)

 * `classes`: An array containing class matcher objects.

 * `eval`: A string specifying Ruby code to run (first) for all matching hooks
   targeting `pkg` application. (optional)

 * `eval_file`: A string specifying the path to a Ruby source file used instead
   of an inline `eval` field. (optional)

