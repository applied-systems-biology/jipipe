$(document).ready(function(){

  // Landing page slideshow
  $('.landing-features').slick({
    infinite: true,
    dots: true,
    slidesToShow: 3,
    slidesToScroll: 3,
    autoplay: true,
    autoplaySpeed: 10000,
    centerMode: true,
    centerPadding: '5rem',
    initialSlide: 1,
    responsive: [
      {
        breakpoint: 1024,
        settings: {
          slidesToShow: 1,
          slidesToScroll: 1
        }
      }
    ]
  });

  // Download page menus
  $(".download-panel-content").css("display", "none");
  $('input:radio[name="download-options"]').change(function(){
    $(".download-panel-content").css("display", "none");
    $("#" + $(this).val() + "-content").css("display", "block");
  });
  $('input:radio[value="download-via-update-site"]').change();
  $('input:radio[value="download-via-update-site"]').prop("checked", true);
});
