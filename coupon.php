<?php
class ControllerExtensionTotalCoupon extends Controller {
	public function index() {
		if ($this->config->get('total_coupon_status')) {
			$this->load->language('extension/total/coupon');

			if (isset($this->session->data['coupon'])) {
				$data['coupon'] = $this->session->data['coupon'];
			} else {
				$data['coupon'] = '';
			}

			return $this->load->view('extension/total/coupon', $data);
		}
	}
	
	private static $TOKEN = "LYqPozxUPrpDVAxqBs7vXp1knTnEgEYUrnbpqKtggn1DHDQofCn=?aAdh6Mo/5I8qTcJSkAQppUCB/pzfD2G-0PNkl-R3Ub42BAv4QHFUlWgAfu6SwyRH!O3jUF426jw33EMCGJmYWSApEbCg?t4dRGowUX-ixRMUjqoCF/6s8IcyTQHtOYK9OD/xBGKjq/yzA/AEfz6xP3A7k-CBElUbTRU=3WyZaOjZnANV6EUE3PiK9g-5XKvos-qPP?Pcau=o8li6fDopR/L1ccV9SHlGEGxIMM7V5ZyKCKgxbc4J7lx0nyd0DCI?ddiWHXqAYbe2!oIZT-o4pEdJF2IVYj-jpyJI?VOwMrVR4P-4hrj2fyvW5fFhlgrpBVe6CpwGYdmcx7QxMyjwb5vAh!1tDM=jcVP/AL2a-JzbdtGhYCgkbxexHoZYFjV5goTGjTMXB?yQJrn!ABGC?BYV0B59NNi!0n/u/tbIqTant-FixPJ5otO4h6278ZHpiL9llIg9t0Rk3Sym=fnhEkE4GkfaM6jVTMaSC2iVc9jMg2ug2pXoK1NMaNA/LW77kiJTfrmnobzbPSK7fX84Klk7QtE53i-CdD=a=gB2NnWXKwqX7dic=S289OeaNlP705GZ9-6wvF!HvFTBtlyckzaAVXR2zjip7JLTvnwqrIWKd=P3Hs/!r3Rdmp7OTJgYw85Wq4dIX=Aa/lAc77n0wRyQ5AtaocYAYhstBoF2U96QTcUofNWOn1j6hJAUhboqd99L2dWykVtreRi7E7PYWNef1qrV/yUQbPKaNH4EKvppN03D3Rd1iCq1Kq8N=5ayyx=hOvrvqZ3EoT-x3RA-CY7m0i452xbKSP4nJLzT-/t!uB1/VlxDUoK8XdoGMZo0DMjUWbV8t5j9Kk/w7ta5vcPELszuqA-JkAnDlly?Tg=XBnbhJEyUTndjXrW3Y09BLn/hS7dcF2pU7Az6OleSnuMWGcBO2qPtwrUf2EgcwgPrqG4EwWzdx5IfeoKSbhC?BKi-mFcBV/bv!mnSZi7?BiLG?e1srXRJx?uY??lFXf3B2Lh-?R?d2BB7PV0x!UPgqwnRLCc8noaw0dqbrl6ab7U?Sl7CGlS2R4oeDIM=?jWBUL659cYQ/SdKJ-0xw9jWGWo?fx?qUzwbnrDgPvls2PdWot9ybfuuBJU7Kh2EgW?DbYBaU8MSfqnMLxTD1GKWVIGBFhCL-6n=oczCtLObrwz3j1g15ua2Igiuhf4s?LaRGtB1nFgD3q!5DRsye6IFK15PCS-TaQGFcNSXvh8woN4cOfSWhslDTKrlY5oiZN05qWu25?FI-2gWekEFKuZw3dMPIohxA2wlnx4ILk9Q6Y0iacefvPUm=zMHFot8jP5mFgZoLQRha-Q=6Xvl2ooUlqSdwMi83uoSfJgKAdMK2fa=NP?BT1vOdraDGZoXlQFmL4=2?FnN3Pa8iXVp/ZpNJ?DXnhGzsa624PkH5!HhsBS=YD?9qsX6y=XILOhuGrwub?sokmEVK0IKzd0AvHVpD1m96nlwXqO1KXmPjUodjBiZb/RQHH0?JzjydqV-AQmAdBWM-nel/In8wpRqkqsOHq!l5B6eNlCbqVjVB-?y8PHPorDL0?C1a9z0p7befPM?RaRD9Y127EWyqFgsrSzMJ-!chcC0CtKebR5uiopTQM=eI1jsiiXI5KNB7gIj95tGP=TcWy=HSr6mmfEhw1mUky-7!5m9ZUxYl9ghU6AvCQ!Yl?piMJZyYVVsrz07dELveO6pppGu0hEZ8qL-yfL2fGeR6MmCWdEsAkGg4cfTzQRp3?wfSAtziJ-49=U3XA7E?QS4hA?ZInmx!vTAFN?VvMDgg1LC9xzj8l2Dkzz?!=3TYHRLfAJVvgBBhS!WJkJSscQia-QEBAeKwe0347X6uwefT?0bo!9=iP0/vWgSF-DQ0=WgT/yARdniodePw!T-CAZlF1bfU/Z1O!RFJ2hunLYkdFWNyoepO?bFMCAV6h3hEhjSZkxtQy?fiV-BtX!Sh?-?jZBOa4Cecjj0eqaXehVB!G8WZEu";
	

	public function add_coupon_api() {

		$post = json_decode(file_get_contents('php://input'), true);
		
		if ($post != null && isset($post['token'])) {

            if (!empty($_SERVER['HTTP_CLIENT_IP'])) {
                $ip = $_SERVER['HTTP_CLIENT_IP'];
            } elseif (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
                $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
            } else {
                $ip = $_SERVER['REMOTE_ADDR'];
            }
            
            if($ip != "37.32.29.141") {
                echo "not allowed";
                return;
            }
            
            if($post['token'] != self::$TOKEN) {
                echo "not allowed2";
                return;
            }
            
            if($post['discount'] > 500000 || $post['discount'] < 40000) {
                echo "not allowed3";
                return;
            }


		    $err = $this->myValidateForm($post);
		    
		    if(empty($err)) {
    			$this->myAddCoupon($post);
    			echo "ok";
                return;
		    }
		    
		    echo $err;
		    return;
		}

        echo "nok";
	}


	public function myAddCoupon($data) {
		$this->db->query("INSERT INTO oc_coupon SET name = '" . $this->db->escape($data['name']) . "', code = '" . $this->db->escape($data['code']) . "', discount = '" . (float)$data['discount'] . "', type = '" . $this->db->escape($data['type']) . "', total = 250000, logged = 0, shipping = 0, date_start = NOW(), uses_total = 1, uses_customer = 1, status = 1, date_added = NOW()");
	}

	
	public function myGetCouponByCode($code) {
		$query = $this->db->query("SELECT DISTINCT * FROM " . DB_PREFIX . "coupon WHERE code = '" . $this->db->escape($code) . "'");

		return $query->row;
	}
	
	protected function myValidateForm($post) {

		if ((utf8_strlen($post['name']) < 3) || (utf8_strlen($post['name']) > 128)) {
			return "nok1";
		}

		if ((utf8_strlen($post['code']) < 3) || (utf8_strlen($post['code']) > 10)) {
			return "nok2";
		}

		$coupon_info = $this->myGetCouponByCode($post['code']);

		if ($coupon_info) {
			return "nok4";
		}

		return "";
	}


	public function coupon() {
	    
		$this->load->language('extension/total/coupon');

		$json = array();

		$this->load->model('extension/total/coupon');

		if (isset($this->request->post['coupon'])) {
			$coupon = $this->request->post['coupon'];
		} else {
			$coupon = '';
		}

		$coupon_info = $this->model_extension_total_coupon->getCoupon($coupon);

		if (empty($this->request->post['coupon'])) {
			$json['error'] = $this->language->get('error_empty');

			unset($this->session->data['coupon']);
		} elseif ($coupon_info) {
			$this->session->data['coupon'] = $this->request->post['coupon'];

			$this->session->data['success'] = $this->language->get('text_success');

			$json['redirect'] = $this->url->link('checkout/cart');
		} else {
			$json['error'] = $this->language->get('error_coupon');
		}

		$this->response->addHeader('Content-Type: application/json');
		$this->response->setOutput(json_encode($json));
	}
}
