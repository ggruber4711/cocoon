cocoon.load("resource://org/apache/cocoon/woody/flow/javascript/woody2.js");

function form1(form) {
    var locale = determineLocale();
    var model = form.getModel();
    model.email = "bar@www.foo.com";
    model.somebool = true;
    model.account = 2;
    model.cowheight = 4;
    model.number1 = 1;
    model.number2 = 3;
    model.birthdate = new java.util.Date();
    
    model.contacts[0].firstname = "Jules";
    model.contacts[1].firstname =  "Lucien";
    model.contacts[2].firstname = "Chris";
    model.drinks = ["Jupiler", "Coca Cola"];

    form.locale = locale;
    form.showForm("form1-display-pipeline");
    print("submitId = " + form.submitId);
    if (form.isValid) {
      print("visa=" + model.visa);  
    } else {
      print("Form is not valid");
    }
    // Store the form as a request attribute, as the view is not
    // flow-aware.
    cocoon.request.setAttribute("form1", form.getWidget());
    cocoon.sendPage("form1-success-pipeline");
}

function selectCar() {
    var form = new Form("forms/carselector_form.xml");
    form.showForm("carselector-view");
    cocoon.request.setAttribute("carselectorform", form.getWidget());
    cocoon.sendPage("carselector-success");
}

function determineLocale() {
    var localeParam = cocoon.request.get("locale");
    if (localeParam != null && localeParam.length > 0) {
        return Packages.org.apache.cocoon.i18n.I18nUtils.parseLocale(localeParam);
    }
    return null;
}
